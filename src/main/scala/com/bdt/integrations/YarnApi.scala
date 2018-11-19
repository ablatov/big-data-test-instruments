package com.bdt.integrations

import com.bdt.utils.HTTPClient
import com.typesafe.scalalogging.LazyLogging
import org.json4s.DefaultFormats
import org.json4s.jackson.JsonMethods.parse
import org.scalatest.concurrent.Eventually

import scala.concurrent.duration._

/**
  * Used Yarn's REST API documentation is here
  * https://hadoop.apache.org/docs/current/hadoop-yarn/hadoop-yarn-site/ResourceManagerRest.html.
  */

object YarnApi extends Eventually with LazyLogging {

  def checkAppInYarnRunning(host: String, port: String, appName: String, timeToWait: Int = 300,
                            sleep: Int = 10): String = {
    val appId =
      eventually(timeout(timeToWait.seconds), interval(sleep.second)) {
        val runningAppsResponse =
          HTTPClient.executeGetRequest(s"http://$host:$port/ws/v1/cluster/apps?states=running,accepted")

        implicit val formats: DefaultFormats.type = DefaultFormats
        val parsedResp = parse(runningAppsResponse)
        val responseList = (parsedResp \\ "app").extract[List[Map[String, Any]]]
        val filtered = responseList.filter(map => map("name") == appName)
        assert(filtered.nonEmpty)

        val id = filtered.head("id").asInstanceOf[String]
        val appState = filtered.head("state")
        if (appState == "ACCEPTED") logger.info(s"$appName is still in ACCEPTED STATE!")
        assert(appState == "RUNNING")
        id
      }
    logger.info(s"$appName is in RUNNING STATE! App id is $appId")
    appId
  }

  def controlRunningAppInYarn(host: String, port: String, appName: String, appId: String, timeToWait: Int = 1800,
                              sleep: Int = 20): Unit = {
    eventually(timeout(timeToWait.seconds), interval(sleep.second)) {
      val runningAppsResponse = HTTPClient.executeGetRequest(s"http://$host:$port/ws/v1/cluster/apps?states")

      implicit val formats: DefaultFormats.type = DefaultFormats
      val parsedResp = parse(runningAppsResponse)
      val responseList = (parsedResp \\ "app").extract[List[Map[String, Any]]]
      val filtered = responseList.filter(map => map("id") == appId)
      assert(filtered.nonEmpty)

      val appState = filtered.head("state")
      logger.info(s"$appName is still $appState!")
      val appFinalState = filtered.head("finalStatus")
      assert(appState == "FINISHED")

      logger.info(s"$appName is still in $appFinalState final state, waiting for SUCCEEDED state!")
      assert(appFinalState == "SUCCEEDED")
    }
    logger.info(s"$appName is FINISHED with SUCCEEDED status!")
  }

  def killAppInYarn(host: String, port: String, appName: String, timeToWait: Int = 90, sleep: Int = 5): Unit = {
    val runningAppsResponse = HTTPClient.executeGetRequest(
      s"http://$host:$port/ws/v1/cluster/apps?states=running,accepted")

    implicit val formats: DefaultFormats.type = DefaultFormats
    val parsedResp = parse(runningAppsResponse)
    val responseList = (parsedResp \\ "app").extract[List[Map[String, Any]]]
    val filtered = responseList.filter(map => map("name") == appName)

    if (filtered.nonEmpty) {
      val appId = filtered.head("id")
      val appState = filtered.head("state")

      if (appState == "ACCEPTED" || appState == "RUNNING") {
        logger.info(s"$appName is in $appState STATE! Need to KILL!")
        val putRequest = s"""{"state":"KILLED"}""".stripMargin
        val putHost = s"http://hbase01.ofd.nov:8088/ws/v1/cluster/apps/$appId/state"
        logger.info(s"######## PutHost == $putHost")

        eventually(timeout(timeToWait.seconds), interval(sleep.second)) {
          logger.info(s"Try to Kill App")
          val killAppResponse = HTTPClient.executePutRequest(
            s"http://$host:$port/ws/v1/cluster/apps/$appId/state", putRequest)
          logger.info(s"KillAppResponse == $killAppResponse")
          val stateResponse = HTTPClient.executeGetRequest(s"http://$host:$port/ws/v1/cluster/apps/$appId/state")
          assert(stateResponse.contains("\"state\":\"KILLED\""))
        }

      }
      logger.info(s"$appName is KILLED!")
    }
  }
}