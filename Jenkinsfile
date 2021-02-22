pipeline {
  agent {
      label 'centos-8'
  }

  environment {
    MAVEN_OPTS='-Xmx1024m -Xms256m -XshowSettings:vm -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn'
    MAVEN_CONFIG = '-B -C -U -e'
  }

  tools {
    maven 'apache-maven-latest'
    jdk 'openjdk-jdk11-latest'
  }

  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }

  stages {

    stage('Build') {
      steps {
        sh 'mvn clean verify -f releng/org.eclipse.cbi.p2repo.analyzers.parent/pom.xml'
      }
    }

  }
}
