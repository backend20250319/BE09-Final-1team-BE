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
                    echo "Checking for changes..."

                    def commandOutput = bat(returnStdout: true, script: 'git diff --name-only HEAD~1 HEAD').trim()
                    def changedFiles = commandOutput.split('\r\n').findAll { line -> !line.contains('>') && line.trim() != '' }
                    echo "Cleaned changed files list: ${changedFiles}"

                    def servicePaths = bat(returnStdout: true, script: "dir /s /b Dockerfile").trim().split('\r\n').collect { it.replace('\\Dockerfile', '') }
                    def workspacePath = env.WORKSPACE
                    def relativeServicePaths = servicePaths.collect { it.replace(workspacePath, '').replaceAll('^\\\\', '') }
                    echo "Found relative service paths (based on Dockerfile): ${relativeServicePaths}"

                    // [수정됨] 이제 Set을 사용하여 중복 없이 모든 변경된 서비스를 담습니다.
                    def changedServices = new HashSet<String>()

                    for (String file in changedFiles) {
                        def windowsStyleFile = file.replace('/', '\\')
                        for (String servicePath in relativeServicePaths) {
                            if (windowsStyleFile.startsWith(servicePath + '\\')) {
                                if (changedServices.add(servicePath)) {
                                     echo "SUCCESS: Detected change in service -> ${servicePath}"
                                }
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
                                            bat 'gradlew.bat clean bootJar'
                                        }
                                    }

                                    def serviceName = servicePath.replace('\\', '/').split('/').last()
                                    def imageName = "berrymas/${serviceName}:${env.BUILD_NUMBER}" // Docker Hub 사용자 이름 확인

                                    stage("Docker Build & Push: ${servicePath}") {
                                        bat "docker build -t ${imageName} ."
                                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub-credentials') {
                                            bat "docker push ${imageName}"
                                        }
                                    }
                                    // 빌드 성공 시, 결과를 기록합니다.
                                    buildResults.succeeded.add(servicePath)
                                } catch (e) {
                                    // 빌드 실패 시, 결과를 기록합니다.
                                    buildResults.failed.add(servicePath)
                                    echo "ERROR during build or push for ${servicePath}: ${e.toString()}"
                                }
                            }
                        }
                    }
                    parallel parallelStages

                    // 실패한 서비스가 하나라도 있으면 전체 빌드를 실패 처리합니다.
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
```

### 어떻게 확인하나요?

1.  위 수정된 `Jenkinsfile`을 GitHub `develop` 브랜치에 푸시해주세요.
2.  **여러 서비스의 코드를 동시에 수정**하고 푸시해보세요. (예: `gateway-service`와 `news-service`)
3.  빌드가 완료된 후, **Console Output** 로그의 **맨 아래**를 확인해보세요. 아래와 같이 깔끔한 **빌드 요약(Build Summary)**이 나타날 것입니다.

```
--- Build Summary ---
✅ Succeeded services: services\gateway-service, services\news-service
---------------------

