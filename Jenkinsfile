pipeline {
    agent {
        label 'linux && maven'
    }
    environment {
        DOCKER_REPO_ADDRESS = 'docker-private.packages.damian.space/autops/engine'
    DOCKER_REPO_CREDS = credentials('autops-nexus-damian-space')
    }
    stages {
        stage('Install') {
            steps {
                withMaven(mavenSettingsConfig: 'autopsMavenSettings'){
                    sh 'mvn clean install'
                }
            }
        }
        stage('Build and Push Commit'){
            when { not { tag "release-*" } }
            steps {
                script {
                    tags = []

                    pom = readMavenPom file: 'pom.xml'
                    tags << 'latest-dev-' + env.BRANCH_NAME

                    env.PROJECT_POM_VERSION = pom.version
                    env.DOCKER_REPO_TAGS = tags.join(',').replaceAll('/','-')
                }

                echo "Building commit on $BRANCH_NAME branch with pom version $PROJECT_POM_VERSION."
                echo "This commit will be pushed with these docker tags: $DOCKER_REPO_TAGS"

                withMaven(mavenSettingsConfig: 'autopsMavenSettings'){
                    sh 'mvn -pl engine-core -am -B compile com.google.cloud.tools:jib-maven-plugin:3.1.2:build -Djib.container.mainClass=dev.jesionek.autops.engine.AccountManagementServerApplication -Djib.to.image=$DOCKER_REPO_ADDRESS -Djib.to.tags=$DOCKER_REPO_TAGS -Djib.to.auth.username=$DOCKER_REPO_CREDS_USR -Djib.to.auth.password=$DOCKER_REPO_CREDS_PSW'
                }
            }
        }
        stage('Build and Push Release'){
            when { tag "release-*" }
            steps {
                script {
                    tags = []

                    pom = readMavenPom file: 'pom.xml'
                    tags << 'latest'
                    tags << pom.version

                    env.PROJECT_POM_VERSION = pom.version
                    env.DOCKER_REPO_TAGS = tags.join(',').replaceAll('/','-')

                    if (!env.TAG_NAME.contains(env.PROJECT_POM_VERSION)){
                        error "Please adjust the pom version to be the same as in the name of the release tag."
                    }
                }

                echo "Building release $TAG_NAME with pom version $PROJECT_POM_VERSION."
                echo "This release will be pushed with these docker tags: $DOCKER_REPO_TAGS"

                withMaven(mavenSettingsConfig: 'autopsMavenSettings'){
                    sh 'mvn -pl engine-core -am -B compile com.google.cloud.tools:jib-maven-plugin:3.1.2:build -Djib.container.mainClass=dev.jesionek.autops.engine.AccountManagementServerApplication -Djib.to.image=$DOCKER_REPO_ADDRESS -Djib.to.tags=$DOCKER_REPO_TAGS -Djib.to.auth.username=$DOCKER_REPO_CREDS_USR -Djib.to.auth.password=$DOCKER_REPO_CREDS_PSW'
                }
            }
        }
    }
    post {
        always {
            junit '*/target/surefire-reports/**/*.xml'
            /*jacoco (
                minimumLineCoverage: "80%"
                //buildOverBuild: true
            )*/
        }
    }
}