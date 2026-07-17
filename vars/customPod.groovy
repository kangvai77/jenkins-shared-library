// vars/customPod.groovy
def call(String image, Closure body) {
    podTemplate(
        namespace: 'pipeline',
        containers: [
            containerTemplate(
                name: 'jnlp',
                image: 'crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/jenkins-inbound-agent:3383.vc8881d4b_0e76-1-jdk25',
		resourceRequestCpu: '200m',
                resourceRequestMemory: '256Mi',
                resourceLimitCpu: '500m',
                resourceLimitMemory: '512Mi'
            ),
            containerTemplate(
                name: 'worker',
                image: image,
                command: 'cat',
                ttyEnabled: true,
		resourceRequestCpu: '100m',
                resourceRequestMemory: '128Mi',
                resourceLimitCpu: '500m',
                resourceLimitMemory: '256Mi'
            )
        ]
    ) {
        body()
    }
}
