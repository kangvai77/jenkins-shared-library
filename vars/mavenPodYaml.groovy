// Maven 构建 Pod Template,支持参数化配置
// 用法示例:
//   yaml mavenPodYaml(image: 'maven:3.9-eclipse-temurin-17')
//   yaml mavenPodYaml(image: 'xxx/maven:3.9', cpuLimit: '2', memLimit: '2Gi', useCache: false)
def call(Map config = [:]) {
    // 默认参数,调用时不传就用这些值
    String mavenImage   = config.get('image', 'maven:3.9-eclipse-temurin-17')
    String jnlpImage    = config.get('jnlpImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/jenkins-inbound-agent:3383.vc8881d4b_0e76-1-jdk25')
    String namespace    = config.get('namespace', 'pipeline')
    String cpuRequest   = config.get('cpuRequest', '500m')
    String memRequest   = config.get('memRequest', '512Mi')
    String cpuLimit     = config.get('cpuLimit', '1')
    String memLimit     = config.get('memLimit', '1Gi')
    Boolean useCache    = config.get('useCache', true)
    String cacheClaim   = config.get('cacheClaimName', 'maven-cache-pvc')

    // 缓存卷:useCache=true 用 PVC(跨构建持久化),false 用 emptyDir(仅本次构建有效)
    String cacheVolume = useCache ?
        "persistentVolumeClaim:\n      claimName: ${cacheClaim}" :
        "emptyDir: {}"

    return """
apiVersion: v1
kind: Pod
metadata:
  namespace: ${namespace}
spec:
  restartPolicy: Never
  containers:
  - name: jnlp
    image: ${jnlpImage}
    imagePullPolicy: IfNotPresent
  - name: maven
    image: ${mavenImage}
    command: ['cat']
    tty: true
    imagePullPolicy: IfNotPresent
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
  volumes:
  - name: maven-cache
    ${cacheVolume}
"""
}
