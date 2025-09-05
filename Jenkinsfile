// Jenkinsfile for Monorepo with multiple microservices (final version with fixes)

pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes..."

                    def commandOutput = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
                    def changedFiles = commandOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }
                    echo "Cleaned changed files list: ${changedFiles}"

                    def servicePaths = bat(returnStdout: true, script: "dir /s /b Dockerfile").trim().split('\r\n').collect { it.replace('\\Dockerfile', '') }
                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = servicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found relative service paths (based on Dockerfile): ${relativeServicePaths}"

                    def changedServices = []

                    for (String file in changedFiles) {
                        def windowsStyleFile = file.replace('/', '\\')
                        for (String servicePath in relativeServicePaths) {
                            if (windowsStyleFile.startsWith(servicePath + '\\') && !changedServices.contains(servicePath)) {
                                changedServices.add(servicePath)
                                echo "SUCCESS: Detected change in service -> ${servicePath}"
                            }
                        }
                    }

                    if (changedServices.isEmpty()) {
                        echo "No changes detected in any service directory. Skipping build."
                        currentBuild.result = 'NOT_BUILT'
                        return
                    }

                    env.CHANGED_SERVICES = changedServices.join(',')
                }
            }
        }

        stage('Build and Push Changed Services') {
            when {
                expression { env.CHANGED_SERVICES != null && env.CHANGED_SERVICES != '' }
            }
            steps {
                script {
                    def changedServicesList = env.CHANGED_SERVICES.split(',')
                    def parallelStages = [:]

                    for (String servicePath in changedServicesList) {
                        parallelStages["Build & Push ${servicePath}"] = {
                            dir(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    stage("Build Application: ${servicePath}") {
                                        if (fileExists('gradlew.bat')) {
                                            echo "Detected Java/Gradle project. Running Gradle build..."
                                            bat 'gradlew.bat clean bootJar'
                                        } else {
                                            echo "Detected non-Gradle project (e.g., Python). Skipping build step."
                                        }
                                    }

                                    // [수정됨] Windows 경로 문자를 안전하게 처리하도록 로직을 변경합니다.
                                    def serviceName = servicePath.replace('\\', '/').split('/').last()
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Verify Docker Environment") {
                                        bat 'docker --version'
                                    }

                                    stage("Docker Build: ${servicePath}") {
                                        bat "docker build -t ${imageName} ."
                                    }
                                    stage("Push Docker Image: ${servicePath}") {
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            bat "docker push ${imageName}"
                                        }
                                    }
                                } catch (e) {
                                    echo "ERROR: An exception occurred during build or push for ${servicePath}"
                                    echo "Caught Exception: ${e.toString()}"
                                    error("Build or push failed for ${servicePath}")
                                }
                            }
                        }
                    }
                    parallel parallelStages
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline finished.'
            cleanWs()
        }
    }
}

