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
                
                sh "mkdir library app"

                dir("library") {
                    git branch: 'resources_folder', url: 'https://github.com/terop1989/k8s-library.git'
                    echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"
                }

                dir('app'){
                    checkout scm
                    release_number = env.TAG_NAME.split('-')[0]
                    sh "docker build -t ${pipelineParams.projectName}:${release_number} ./app/"
                }
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