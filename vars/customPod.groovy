// vars/customPod.groovy
def call(String image) {
    return """
apiVersion: v1
kind: Pod
metadata:
  namespace: pipeline
spec:
  restartPolicy: Never
  containers:
  - name: jnlp
    image: crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/jenkins-inbound-agent:3383.vc8881d4b_0e76-1-jdk25
  - name: worker
    image: ${image}
    command: ['cat']
    tty: true
"""
}
