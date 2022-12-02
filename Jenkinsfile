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
    jdk 'openjdk-latest'
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
      post {
		always {
			junit '**/target/surefire-reports/TEST-*.xml'
			archiveArtifacts artifacts: 'releng/org.eclipse.cbi.p2repo.analyzers.repository/target/**, releng/org.eclipse.cbi.p2repo.analyzers.product/target/**'
			
		}
	  }
    }
    stage('Deploy') {
      when {
        branch 'main'
	  }
      steps {
		sshagent ( ['projects-storage.eclipse.org-bot-ssh']) {
					sh '''
						ssh genie.cbi@projects-storage.eclipse.org rm -rf /home/data/httpd/download.eclipse.org/cbi/updates/analyzers/snapshot
						ssh genie.cbi@projects-storage.eclipse.org mkdir -p /home/data/httpd/download.eclipse.org/cbi/updates/analyzers/snapshot
						scp -r releng/org.eclipse.cbi.p2repo.analyzers.repository/target/repository/* genie.cbi@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/cbi/updates/analyzers/snapshot
						scp -r releng/org.eclipse.cbi.p2repo.analyzers.product/target/products/*.tar.gz genie.cbi@projects-storage.eclipse.org:/home/data/httpd/download.eclipse.org/cbi/updates/analyzers/snapshot
					'''
		}
      }
    }
  }
}
