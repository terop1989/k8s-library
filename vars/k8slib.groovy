def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    throttleCategory = 'example'
    throttle([throttleCategory]) {

        node('master') {

            cleanWs()

            stage('Build App Image') {

                sh "mkdir app"

                dir('app'){

                    checkout scm
                    echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"
                    release_number = env.TAG_NAME.split('-')[0]

                    DockerRepositoryAddress='docker.io'

                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh """
                        docker login ${DockerRepositoryAddress} -u $DOCKER_USER -p $DOCKER_PASSWORD
                        docker build -t ${DOCKER_USER}/${pipelineParams.projectName}:${release_number} ./app/
                        docker push     ${DOCKER_USER}/${pipelineParams.projectName}:${release_number}
                        """
                    }
                }
            }

            stage('Deploy to k8s') {

                sh "mkdir library"

                dir("library") {

                    git branch: 'resources_folder', url: 'https://github.com/terop1989/k8s-library.git'

                    HelmAgentDockerfileName = 'resources/k8s/helm-agent.dockerfile'
                    HelmAgentBuildName = 'agent:latest'
                    HelmAgentBuildArgs = ''
                    HelmAgentRunArgs = " -u 0:0"

                    def RunAgent = docker.build("${HelmAgentBuildName}", "${HelmAgentBuildArgs} -f ${HelmAgentDockerfileName} .")
                }
            }

            stage('Cleanup') {
                deleteDir()
            }
        }
    }
}