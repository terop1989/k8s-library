def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    throttleCategory = 'example'
    throttle([throttleCategory]) {

        node('master') {

            cleanWs()

            app_name = "${pipelineParams.projectName}"
            app_namespace = "flask-ns"
            k8s_suffix = "local.con"

            DockerRepositoryAddress='docker.io'
            release_number = env.TAG_NAME.split('-')[0]

            stage('Build App Image') {

                sh "mkdir app"

                dir('app'){

                    checkout scm
                    echo "Branch name is ${env.BRANCH_NAME}\nTag name is ${env.TAG_NAME}"

                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
                        sh """
                        docker login ${DockerRepositoryAddress} -u $DOCKER_USER -p $DOCKER_PASSWORD
                        docker build -t ${DockerRepositoryAddress}/${DOCKER_USER}/${app_name}:${release_number} ./app/
                        docker push     ${DockerRepositoryAddress}/${DOCKER_USER}/${app_name}:${release_number}
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

                    withCredentials([
                        usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD'),
                        file(credentialsId: 'k8s-config', variable: 'K8S_CONFIG')
                        ]){
                            RunAgent.inside("${HelmAgentRunArgs}") {
                                sh  """
                                    mkdir -p ~/.kube
                                    cat ${K8S_CONFIG} > ~/.kube/config
                                    sed -i 's/app-name/${app_name}/' resources/k8s/helm/Chart.yaml
                                    sed -i 's/1.0.0/${release_number}/' resources/k8s/helm/Chart.yaml
                                    helm upgrade ${app_name} resources/k8s/helm/ -n ${app_namespace} \
                                    --set imageCredentials.registry=${DockerRepositoryAddress} \
                                    --set imageCredentials.username=${DOCKER_USER} \
                                    --set imageCredentials.password=${DOCKER_PASSWORD} \
                                    --set container.image=${DockerRepositoryAddress}/${DOCKER_USER}/${app_name}:${release_number} \
                                    --set ingress.host=${app_name}.${k8s_suffix} \
                                    --create-namespace \
                                    --install
                                    """
                                }
                           }
                }
            }

            stage('Cleanup') {
                deleteDir()
            }
        }
    }
}