def call(Map config = [:]) {
    String mavenImage   = config.get('image', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/maven:3.9-eclipse-temurin-21')
    String jnlpImage    = config.get('jnlpImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/jenkins-inbound-agent:3383.vc8881d4b_0e76-1-jdk25')
    String kanikoImage  = config.get('kanikoImage', 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/kaniko-executor:v1.23.2-debug')
    String namespace    = config.get('namespace', 'pipeline')
    String cpuRequest   = config.get('cpuRequest', '500m')
    String memRequest   = config.get('memRequest', '512Mi')
    String cpuLimit     = config.get('cpuLimit', '1')
    String memLimit     = config.get('memLimit', '1Gi')
    Boolean useCache    = config.get('useCache', true)
    String cacheClaim   = config.get('cacheClaimName', 'maven-cache-pvc')
    String acrSecret    = config.get('acrSecretName', 'acr-secret')

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
  - name: kaniko
    image: ${kanikoImage}
    command: ['/busybox/cat']
    tty: true
    imagePullPolicy: IfNotPresent
    volumeMounts:
    - name: docker-config
      mountPath: /kaniko/.docker
  volumes:
  - name: maven-cache
    ${cacheVolume}
  - name: docker-config
    secret:
      secretName: ${acrSecret}
      items:
      - key: .dockerconfigjson
        path: config.json
"""
}
