// vars/updateGitOpsRepo.groovy
// 更新 GitOps 配置仓库里的镜像 tag,并提交推送
// 用法示例:
//   updateGitOpsRepo(
//       repoUrl: 'https://gitlab.com/xxx/simple-java-maven-app-manifests.git',
//       image: "${IMAGE_NAME}:${env.IMAGE_TAG}",
//       credentialsId: 'gitlab-token'
//   )
def call(Map config = [:]) {
    // ============ 必填参数校验 ============
    if (!config.repoUrl) {
        error "updateGitOpsRepo: 缺少必填参数 'repoUrl'"
    }
    if (!config.image) {
        error "updateGitOpsRepo: 缺少必填参数 'image'"
    }

    // ============ 参数读取(带默认值) ============
    String repoUrl          = config.repoUrl
    String image            = config.image
    String credentialsId    = config.get('credentialsId', 'gitlab-token')
    String branch           = config.get('branch', 'main')
    String manifestFile     = config.get('manifestFile', 'deployment.yaml')
    String imageKeyPattern  = config.get('imageKeyPattern', 'image:.*')   // 用于 sed 匹配的行
    String gitUserEmail     = config.get('gitUserEmail', 'jenkins@ci.local')
    String gitUserName      = config.get('gitUserName', 'Jenkins CI')
    String cloneDir         = config.get('cloneDir', 'gitops-manifests')
    String commitMsgPrefix  = config.get('commitMsgPrefix', '更新镜像至')

    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'GIT_USER',
        passwordVariable: 'GIT_TOKEN'
    )]) {
        sh """
            set -e
            rm -rf ${cloneDir}

            # 把 https:// 后面拼上账号密码,实现免交互认证 clone
            AUTH_URL=\$(echo "${repoUrl}" | sed "s#https://#https://\${GIT_USER}:\${GIT_TOKEN}@#")

            git clone --branch ${branch} --depth 1 "\${AUTH_URL}" ${cloneDir}
            cd ${cloneDir}

            sed -i "s|${imageKeyPattern}|image: ${image}|g" ${manifestFile}

            git config user.email "${gitUserEmail}"
            git config user.name "${gitUserName}"

            if git diff --quiet; then
                echo "配置无变化,跳过提交"
            else
                git add ${manifestFile}
                git commit -m "${commitMsgPrefix} ${image}"
                git push origin ${branch}
                echo "✅ 已更新 ${manifestFile} 并推送"
            fi
        """
    }
}