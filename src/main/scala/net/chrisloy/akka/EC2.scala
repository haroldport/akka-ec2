package net.chrisloy.akka

import com.amazonaws.services.ec2.AmazonEC2Client
import java.net.URL
import java.io.{InputStreamReader, BufferedReader}
import com.amazonaws.services.ec2.model.{InstanceStateName, DescribeInstancesRequest, Instance}
import scala.collection.JavaConversions._
import com.amazonaws.services.autoscaling.model.{DescribeAutoScalingGroupsRequest, DescribeAutoScalingInstancesRequest}
import com.amazonaws.services.autoscaling.AmazonAutoScalingClient

class EC2(scaling: AmazonAutoScalingClient, ec2: AmazonEC2Client) {

  def siblingIps: List[String] = groupInstanceIds(groupName(instanceId)) map instanceFromId collect {
    case instance if isRunning(instance) => instance.getPrivateIpAddress
  }

  def currentIp = instanceFromId(instanceId).getPrivateIpAddress

  val isRunning: Instance => Boolean = _.getState.getName == InstanceStateName.Running.toString

  private def instanceId = {
    val conn = new URL("http://54.89.35.191/latest/meta-data/instance-id").openConnection
    val in = new BufferedReader(new InputStreamReader(conn.getInputStream))
    println(s"========<<<<>>>>>>---========== ${conn} , $in, --> ${in.readLine()}")
    try in.readLine() finally in.close()
  }

  private def instanceFromId(id: String): Instance = {
    val result = ec2 describeInstances new DescribeInstancesRequest {
      setInstanceIds(id :: Nil)
    }
    result.getReservations.head.getInstances.head
  }

  private def groupName(instanceId: String) = {
    val result = scaling describeAutoScalingInstances new DescribeAutoScalingInstancesRequest {
      setInstanceIds(instanceId :: Nil)
    }
    println(s"result head ----->>>>>>>>>> ${result.getAutoScalingInstances.head.getAutoScalingGroupName}")
    result.getAutoScalingInstances.head.getAutoScalingGroupName
  }

  private def groupInstanceIds(groupName: String) = {
    val result = scaling describeAutoScalingGroups new DescribeAutoScalingGroupsRequest {
      setAutoScalingGroupNames(groupName :: Nil)
    }
    result.getAutoScalingGroups.head.getInstances.toList map (_.getInstanceId)
  }
}