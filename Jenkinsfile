// Jenkinsfile for Monorepo with multiple microservices (final version with summary)

// 빌드 결과를 저장하기 위한 전역 변수를 선언합니다.
def buildResults = [succeeded: [], failed: []]

pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    stages {
        stage('Detect Changed Services') {
            steps {
                script {
                    echo "Checking for changes using Jenkins changelog..."

                    def changedServices = new HashSet<String>()

                    def servicePathsOutput = bat(returnStdout: true, script: "dir /s /b Dockerfile").trim()
                    def allServicePaths = servicePathsOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }.collect { it.replace('\\Dockerfile', '') }

                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = allServicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found all relative service paths (based on Dockerfile): ${relativeServicePaths}"

                    // ======================================================================
                    // [수정됨] 수동 git diff 대신 Jenkins의 내장 'changeSets'를 사용하여 변경된 파일을 감지합니다.
                    // 이것이 가장 안정적이고 정확한 방법입니다.
                    // ======================================================================
                    if (currentBuild.changeSets.isEmpty()) {
                        echo "No changesets found. This might be a manual build or the first build. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else {
                        echo "Found ${currentBuild.changeSets.size()} changesets."
                        // 각 changeset (보통 하나)을 순회합니다.
                        for (changeSet in currentBuild.changeSets) {
                            echo "Processing changeset with ${changeSet.items.length} commits."
                            // changeset 안의 각 커밋을 순회합니다.
                            for (item in changeSet.items) {
                                echo "Commit ${item.commitId} by ${item.author}: ${item.msg}"
                                // 커밋에 의해 영향을 받은 각 파일 경로를 순회합니다.
                                for (path in item.affectedPaths) {
                                    echo "  - Changed file: ${path}"
                                    def windowsStyleFile = path.replace('/', '\\')

                                    for (String servicePath in relativeServicePaths) {
                                        if (windowsStyleFile.startsWith(servicePath + '\\')) {
                                            if (changedServices.add(servicePath)) {
                                                 echo "SUCCESS: Detected change in service -> ${servicePath}"
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    echo "Final list of changed services to be built: ${changedServices.toList()}"

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
                    echo "Build stage received the following services: ${env.CHANGED_SERVICES}"
                    def changedServicesList = env.CHANGED_SERVICES.split(',')
                    echo "Services list after splitting: ${changedServicesList}"

                    def parallelStages = [:]

                    for (String servicePath in changedServicesList) {
                        parallelStages["Build & Push ${servicePath}"] = {
                            dir(servicePath) {
                                echo "--- Starting build & push for ${servicePath} ---"
                                try {
                                    stage("Build Application: ${servicePath}") {
                                        if (fileExists('gradlew.bat')) {
                                            bat 'gradlew.bat clean bootJar --no-daemon'
                                        }
                                    }

                                    def serviceName = servicePath.replace('\\', '/').split('/').last()
                                    def imageName = "heechae15/${serviceName}:${env.BUILD_NUMBER}"

                                    stage("Docker Build & Push: ${servicePath}") {
                                        bat "docker build -t ${imageName} ."
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            bat "docker push ${imageName}"
                                        }
                                    }
                                    buildResults.succeeded.add(servicePath)
                                } catch (e) {
                                    buildResults.failed.add(servicePath)
                                    echo "ERROR during build or push for ${servicePath}: ${e.toString()}"
                                }
                            }
                        }
                    }
                    parallel parallelStages

                    if (!buildResults.failed.isEmpty()) {
                        error("One or more services failed to build: ${buildResults.failed.join(', ')}")
                    }
                }
            }
        }
    }

    post {
        always {
            script {
                echo '--- Build Summary ---'
                if (!buildResults.succeeded.isEmpty()) {
                    echo "✅ Succeeded services: ${buildResults.succeeded.join(', ')}"
                }
                if (!buildResults.failed.isEmpty()) {
                    echo "❌ Failed services: ${buildResults.failed.join(', ')}"
                }
                if (currentBuild.result == 'NOT_BUILT') {
                    echo "- No services were built as no changes were detected."
                }
                echo '---------------------'
                cleanWs()
            }
        }
    }
}