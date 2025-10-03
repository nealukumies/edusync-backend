pipeline {
    agent any
    tools {
        maven 'MAVEN_HOME'
    }

    environment {
        DB_CRED = credentials('edusync-db-user')
        PORT = "8000"

        PATH = "C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"


        DOCKERHUB_CREDENTIALS_ID = 'Docker_Hub'
        DOCKERHUB_REPO = 'oonnea/otp1_edusync_backend'
        DOCKER_IMAGE_TAG = 'latest'


    }

    stages {

    stage('Prepare .env') {
        steps {
            bat """
            echo DB_URL=jdbc:mariadb://db4free.net:3306/edusync > .env
            echo DB_USER=%DB_CRED_USR% >> .env
            echo DB_PASSWORD=%DB_CRED_PSW% >> .env
            echo PORT=%PORT% >> .env
            """
        }
    }
        stage('checking') {
            steps {
                git branch: 'main', url: 'https://github.com/nealukumies/edusync-backend.git'
            }
        }

        stage('build') {
            steps {
                bat 'mvn clean install'
            }
        }

        stage('Test') {
            steps {
                bat "mvn test -Ddb.url=${env.DB_URL} -Ddb.user=${env.DB_CRED_USR} -Ddb.pass=${env.DB_CRED_PSW}"
            }
        }

        stage('Code Coverage') {
            steps {
                bat 'mvn jacoco:report'
            }
        }

        stage('Publish Test Results') {
            steps {
                junit '**/target/surefire-reports/*.xml'
            }
        }

        stage('Publish Coverage Report') {
            steps {
                jacoco()
            }
        }

                stage('Upload Coverage to WebDisk') {
                    steps {
                        withCredentials([usernamePassword(credentialsId: 'metroweb', usernameVariable: 'WEB_USER', passwordVariable: 'WEB_PASS')]) {
                            bat """
                                powershell -Command ^
                                \$ReportDir = 'target\\\\site\\\\jacoco'; ^
                                \$TargetUrl = 'https://users.metropolia.fi/~neal/otp1/coverage/${BUILD_NUMBER}/'; ^
                                ^
                                # Create directory (MKCOL) ^
                                curl -u $env:WEB_USER + ':' + $env:WEB_PASS -X MKCOL \$TargetUrl || Write-Host 'Directory may already exist'; ^
                                ^
                                # Loop through all files recursively and upload ^
                                Get-ChildItem -Path \$ReportDir -Recurse -File | ForEach-Object { ^
                                    \$RelativePath = $_.FullName.Substring(\$ReportDir.Length + 1) -replace '\\\\','/'; ^
                                    \$DestUrl = \$TargetUrl + \$RelativePath; ^
                                    # Ensure subfolders exist ^
                                    \$DestDir = [System.IO.Path]::GetDirectoryName(\$DestUrl); ^
                                    curl -u $env:WEB_USER + ':' + $env:WEB_PASS -X MKCOL \$DestDir || Write-Host 'Subfolder may already exist'; ^
                                    # Upload file ^
                                    curl -u $env:WEB_USER + ':' + $env:WEB_PASS -T $_.FullName \$DestUrl; ^
                                }
                            """
                        }
                    }
                }



        stage('Build Docker Image') {
            steps {
                script {
                    bat "docker build -t ${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG} ."
                }
            }
        }

        stage('Push Docker Image to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: DOCKERHUB_CREDENTIALS_ID,
                    usernameVariable: 'DOCKERHUB_USERNAME',
                    passwordVariable: 'DOCKERHUB_PASSWORD'
                )]) {
                    script {
                        bat "echo %DOCKERHUB_PASSWORD% | docker login -u %DOCKERHUB_USERNAME% --password-stdin"
                        bat "docker push ${DOCKERHUB_REPO}:${DOCKER_IMAGE_TAG}"
                    }
                }
            }
        }
    }
}