# QA practice INF-433: Jenkins controller that can also run the Selenium suite.
# It adds Chromium + its matching driver, because the stock Jenkins image has no browser.
#
#   docker build -t jenkins-qa qa/jenkins
#   docker run -d --name jenkins-qa -p 8080:8080 -p 50000:50000 -v jenkins_qa_home:/var/jenkins_home jenkins-qa
FROM jenkins/jenkins:lts-jdk21

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium chromium-driver git \
    && rm -rf /var/lib/apt/lists/*

# Read by OwnerFlowSeleniumTest, so Selenium uses these instead of downloading a driver.
ENV CHROME_BIN=/usr/bin/chromium
ENV CHROMEDRIVER=/usr/bin/chromedriver

USER jenkins
