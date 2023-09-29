def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    throttleCategory = 'example'
    throttle([throttleCategory]) {

        node('master') {

            cleanWs()

            stage('Checkout SCM') {
                checkout scm
                echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"
            }

            def ResourceFolder = libraryResource 'folder1'
                ResourceFolderWS = 'folder1.ws'
                writeFile file: ResourceFolderWS , text: ResourceFolder

                sh "ls -la"

            stage('Cleanup') {
                deleteDir()
            }
        }
    }
}