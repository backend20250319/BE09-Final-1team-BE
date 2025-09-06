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

                    if (currentBuild.changeSets.isEmpty()) {
                        echo "No changesets found. This might be a manual build or the first build. Building all services."
                        changedServices.addAll(relativeServicePaths)
                    } else {
                        echo "Found ${currentBuild.changeSets.size()} changesets."
                        for (changeSet in currentBuild.changeSets) {
                            echo "Processing changeset with ${changeSet.items.length} commits."
                            for (item in changeSet.items) {
                                echo "Commit ${item.commitId} by ${item.author}: ${item.msg}"
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

                                    // ======================================================================
                                    // [FIXED] 'synchronized' 블록을 추가하여 공유 변수(buildResults)에 대한
                                    // 동시 접근을 제어하고 데이터 무결성을 보장합니다.
                                    // ======================================================================
                                    synchronized(buildResults) {
                                        buildResults.succeeded.add(servicePath)
                                    }
                                } catch (e) {
                                    // [FIXED] 여기도 마찬가지로 synchronized 블록을 추가합니다.
                                    synchronized(buildResults) {
                                        buildResults.failed.add(servicePath)
                                    }
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