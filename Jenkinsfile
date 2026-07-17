@Library('my-shared-library') _

pipeline {
    agent {
        kubernetes {
            yaml customPodYaml('crpi-whdz2l2sopzelm2i-vpc.cn-beijing.personal.cr.aliyuncs.com/kangvai/busybox:latest')
        }
    }
    stages {
        stage('压力测试连接稳定性') {
            steps {
                container('worker') {
                    sh '''
                        for i in $(seq 1 1000); do
                            echo "第 $i 行日志"
                        done
                        sleep 120
                    '''
                }
            }
        }
    }
}
