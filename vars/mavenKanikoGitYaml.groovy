// vars/mavenKanikoGitYaml.groovy
def call(Map config = [:]) {
    String namespace    = config.get('namespace', 'pipeline')
    String imagePullPolicy = config.get('imagePullPolicy', 'IfNotPresent')
    String mavenImage   = config.get('image', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/maven:3.9-eclipse-temurin-21')
    String jnlpImage    = config.get('jnlpImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/jenkins-inbound-agent:3383.vc8881d4b_0e76-1-jdk25')
    String kanikoImage  = config.get('kanikoImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/kaniko-executor:v1.23.2-debug')
    String gitImage     = config.get('gitImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/alpine-git:latest')
    
    // maven 容器资源
    String cpuRequest   = config.get('cpuRequest', '500m')
    String memRequest   = config.get('memRequest', '512Mi')
    String cpuLimit     = config.get('cpuLimit', '1')
    String memLimit     = config.get('memLimit', '1Gi')

    // jnlp 容器资源（Jenkins agent 本体，负责和 master 通信，负载较轻）
    String jnlpCpuRequest = config.get('jnlpCpuRequest', '100m')
    String jnlpMemRequest = config.get('jnlpMemRequest', '256Mi')
    String jnlpCpuLimit   = config.get('jnlpCpuLimit', '500m')
    String jnlpMemLimit   = config.get('jnlpMemLimit', '512Mi')

    // kaniko 容器资源（镜像构建通常吃资源较多，尤其是大项目/多层镜像）
    String kanikoCpuRequest = config.get('kanikoCpuRequest', '500m')
    String kanikoMemRequest = config.get('kanikoMemRequest', '512Mi')
    String kanikoCpuLimit   = config.get('kanikoCpuLimit', '2')
    String kanikoMemLimit   = config.get('kanikoMemLimit', '2Gi')

    // git 容器资源
    String gitCpuRequest = config.get('gitCpuRequest', '100m')
    String gitMemRequest = config.get('gitMemRequest', '128Mi')
    String gitCpuLimit   = config.get('gitCpuLimit', '300m')
    String gitMemLimit   = config.get('gitMemLimit', '256Mi')

    Boolean useCache    = config.get('useCache', false)
    String cacheClaim   = config.get('cacheClaimName', 'maven-cache-pvc')
    String acrSecret    = config.get('acrSecretName', 'acr-secret')

    // Pod 整体运行超时兜底（秒），防止构建 hang 住长期占用节点资源
    Integer activeDeadlineSeconds = config.get('activeDeadlineSeconds', 3600)

    // 调度相关：节点选择器与容忍度，便于调度到专用构建节点池
    Map nodeSelector = config.get('nodeSelector', [:])
    List tolerations = config.get('tolerations', [])
    
    // workspace 临时卷大小限制，防止异常构建写爆节点磁盘
    String workspaceSizeLimit = config.get('workspaceSizeLimit', '5Gi')

    String cacheVolume = useCache ?
        "persistentVolumeClaim:\n      claimName: ${cacheClaim}" :
        "emptyDir: {}"

    String nodeSelectorYaml = nodeSelector ?
        "  nodeSelector:\n" + nodeSelector.collect { k, v -> "    ${k}: \"${v}\"" }.join("\n") + "\n" : ""
 
    String tolerationsYaml = tolerations ?
        "  tolerations:\n" + tolerations.collect { t ->
            "  - key: \"${t.key}\"\n    operator: \"${t.operator ?: 'Equal'}\"\n    value: \"${t.value ?: ''}\"\n    effect: \"${t.effect}\""
        }.join("\n") + "\n" : ""

    return """
apiVersion: v1
kind: Pod
metadata:
  namespace: ${namespace}
spec:
  restartPolicy: Never
  activeDeadlineSeconds: ${activeDeadlineSeconds}
  imagePullSecrets:
  - name: ${acrSecret}
${nodeSelectorYaml}${tolerationsYaml}  containers:
  - name: jnlp
    image: ${jnlpImage}
    imagePullPolicy: "${imagePullPolicy}"
    resources:
      requests:
        cpu: "${jnlpCpuRequest}"
        memory: "${jnlpMemRequest}"
      limits:
        cpu: "${jnlpCpuLimit}"
        memory: "${jnlpMemLimit}"
  - name: maven
    image: ${mavenImage}
    command: ['cat']
    tty: true
    imagePullPolicy: "${imagePullPolicy}"
    resources:
      requests:
        cpu: "${cpuRequest}"
        memory: "${memRequest}"
      limits:
        cpu: "${cpuLimit}"
        memory: "${memLimit}"
    volumeMounts:
    - name: maven-cache
      mountPath: /root/.m2
  - name: kaniko
    image: ${kanikoImage}
    command: ['/busybox/cat']
    tty: true
    imagePullPolicy: "${imagePullPolicy}"
    resources:
      requests:
        cpu: "${kanikoCpuRequest}"
        memory: "${kanikoMemRequest}"
      limits:
        cpu: "${kanikoCpuLimit}"
        memory: "${kanikoMemLimit}"
    volumeMounts:
    - name: docker-config
      mountPath: /kaniko/.docker
  - name: git
    image: ${gitImage}
    command: ['cat']
    tty: true
    imagePullPolicy: "${imagePullPolicy}"
    resources:
      requests:
        cpu: "${gitCpuRequest}"
        memory: "${gitMemRequest}"
      limits:
        cpu: "${gitCpuLimit}"
        memory: "${gitMemLimit}"
  volumes:
  - name: maven-cache
    ${cacheVolume}
  - name: docker-config
    secret:
      secretName: ${acrSecret}
      items:
      - key: .dockerconfigjson
        path: config.json
  - name: workspace-volume
    emptyDir:
      sizeLimit: ${workspaceSizeLimit}
"""
}