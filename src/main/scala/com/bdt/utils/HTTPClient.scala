package com.bdt.utils

import java.io.{BufferedReader, InputStreamReader}

import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.methods._
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{BasicCookieStore, CloseableHttpClient, HttpClientBuilder}


object HTTPClient extends LazyLogging {

  val cookieStore = new BasicCookieStore

  def createClient(): CloseableHttpClient = {
    HttpClientBuilder.create
      .setDefaultCookieStore(cookieStore)
      .build
  }

  private def executeRequest(request: HttpUriRequest) = {
    createClient().execute(request)
  }

  private def getOrDeleteRequest(url: String, requestType: String, headers: Map[String, String], rcExpected: Int) = {
    var request: HttpUriRequest = null
    if (requestType == "DELETE") request = new HttpDelete(url)
    else request = new HttpGet(url)
    if (headers.nonEmpty)
      for ((k, v) <- headers)
        request.addHeader(k, v)
    val response = executeRequest(request)
    val br = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    if (response.getStatusLine.getStatusCode != rcExpected)
      throw new RuntimeException(
        s"Failed : HTTP error code : ${response.getStatusLine.getStatusCode} Response: ${br.readLine}")
    response
  }

  private def executePostOrPutRequest(url: String, requestType: String, headers: Map[String, String],
                                      body: String, contentType: String, rcExpected: Int) = {
    var request: HttpEntityEnclosingRequestBase = null
    if (requestType == "PUT") request = new HttpPut(url)
    else request = new HttpPost(url)
    val input = new StringEntity(body, "UTF-8")
    input.setContentType(contentType)
    request.setEntity(input)
    if (headers.nonEmpty)
      for ((k, v) <- headers)
        request.addHeader(k, v)
    val response = executeRequest(request)
    val br = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    if (response.getStatusLine.getStatusCode != rcExpected)
      throw new RuntimeException(
        s"Failed : HTTP error code : ${response.getStatusLine.getStatusCode} Response: ${br.readLine}")
    response
  }

  def executeGetRequest(url: String, rcExpected: Int = 200, headers: Map[String, String] = Map.empty): String = {
    val response = getOrDeleteRequest(url, "GET", headers, rcExpected)
    val br = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    val output = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    if (output == "") null
    else output
  }

  def executePostRequest(url: String, body: String, rcExpected: Int = 200, headers: Map[String, String] = Map.empty,
                         contentType: String = "application/json"): String = {
    val response = executePostOrPutRequest(url, "POST", headers, body, contentType, rcExpected)
    val br = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    val output = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    if (output == "") null
    else output
  }

  def executePutRequest(url: String, body: String, rcExpected: Int = 200, headers: Map[String, String] = Map.empty,
                        contentType: String = "application/json"): String = {
    val response = executePostOrPutRequest(url, "PUT", headers, body, contentType, rcExpected)
    val br = new BufferedReader(new InputStreamReader(response.getEntity.getContent))
    val output = Stream.continually(br.readLine()).takeWhile(_ != null).mkString("\n")
    if (output == "") null
    else output
  }
}