def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    throttleCategory = 'example'
    throttle([throttleCategory]) {

        node('master') {

            cleanWs()

            sh "mkdir app"

            dir("app") {

                stage('Checkout SCM') {
                    checkout scm
                    echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"
                }
            }

            sh "pwd"
            sh "ls -la"

                writeFile file: 'k8s-agent.dockerfile', text:  (libraryResource 'k8s-agent.dockerfile')
                k8sAgentBuildName = 'k8s-agent:latest'
                k8sAgentBuildArgs = ''
                k8sAgentRunArgs = '-u 0:0'
            
            def k8sAgent = docker.build("${k8sAgentBuildName}", "${k8sAgentBuildArgs} -f ${k8sAagentFile} .")

            stage('Cleanup') {
                deleteDir()
            }
        }
    }
}