#!/usr/bin/python

import sys
from mininet.net import Mininet
from mininet.topo import Topo
from mininet.log import setLogLevel
from mininet.cli import CLI
from mininet.link import Link,TCLink,Intf
from mininet.examples.cluster import MininetCluster, SwitchBinPlacer
from mininet.examples.clustercli import ClusterCLI as CLI

from mininet.node import RemoteController, OVSKernelSwitch, Host

k = 4
fatTreeLayer = 2
numCoreSwitch = (k/2)*(k/2)
numAggSwitch = k/2
numHosts = k/2

numTotalCoreSwitch = numCoreSwitch
numTotalAggSwitch = numAggSwitch*2*k
numTotalHosts = numHosts*numAggSwitch*k

arrayCoreSwitches = []
arrayAggSwitch = [[[]]] # index1: pod, 2: fattreelayer, 3: switchindex
arrayHosts = [[[]]] # index1: pod, 2: aggSwitchIndex, 3: hostindex

class MyTopo( Topo):
	def __init__( self ):

		Topo.__init__ ( self )


		for index1 in range (0, k):
			arrayAggSwitch.insert(index1, [[]])
			for index2 in range (0, 2):
				arrayAggSwitch[index1].insert(index2, [])

		for index1 in range (0, k):
			arrayHosts.insert(index1, [[]])
			for index2 in range (0, numAggSwitch):
				arrayHosts[index1].insert(index2, [])

		for j in range (0, k/2):
			for i in range (0, k/2):
				swIndex = "s1_" + str(j*(k/2)+i)
				tempDPID = "01:00:00:00:00:"
				if k < 10:
					tempDPID = tempDPID + "0" + str(k) +":"
				else:
					tempDPID = tempDPID + str(k) +":"
	
				if j < 10:
					tempDPID = tempDPID + "0" + str(j) +":"
				else:
					tempDPID = tempDPID + str(j) +":"

				if i < 10:
					tempDPID = tempDPID + "0" + str(i)
				else:
					tempDPID = tempDPID + str(i)
	
				tempSW = self.addSwitch( swIndex, cls=OVSKernelSwitch, protocols='OpenFlow13', dpid=tempDPID)

				arrayCoreSwitches.insert(j*(k/2)+i, tempSW)



		for i in range (0, k):
			for index in range (0, 2*numAggSwitch):
				swIndex = "s1_" + str(numCoreSwitch + i*2*numAggSwitch + index)
				tempDPID = "01:00:00:00:00:"
				if i < 10:
					tempDPID = tempDPID + "0" + str(i) + ":"
				else:
					tempDPID = tempDPID + str(i) + ":"

				if index < 10:
					tempDPID = tempDPID + "0" + str(index) + ":01"
				else:
					tempDPID = tempDPID + str(index) + ":01"
				tempSW = self.addSwitch( swIndex, cls=OVSKernelSwitch, protocols='OpenFlow13', dpid=tempDPID)

				if index < numAggSwitch:
					arrayAggSwitch[i][0].insert(0, tempSW)
				else:
					arrayAggSwitch[i][1].insert(index-numAggSwitch, tempSW)
	
		for pod in range (0, k):
			for switch in range (0, numAggSwitch):
				for index in range (0, k/2):
					hostIndex = "h1_" + str(pod*numAggSwitch*(k/2) + switch*(k/2) + index)
					tempIP = "10." + str(pod+10) + "." + str(switch) + "." + str(index)
					macAddr = '91:00:00:00:00:' + str(pod*numAggSwitch*(k/2) + switch*(k/2) + index)
					print macAddr
					tempHost = self.addHost(hostIndex, ip=tempIP, mac=macAddr)
					arrayHosts[pod][switch].insert(index, tempHost)

		for pod in range (0, k):
			for indexsw in range (0, numAggSwitch):
				for i in range (0, k/2):
					self.addLink(arrayAggSwitch[pod][0][indexsw], arrayCoreSwitches[indexsw*(k/2)+i])


		for pod in range (0, k):
			for indexsw1 in range (0, numAggSwitch):
				for indexsw2 in range (0, numAggSwitch):
					self.addLink(arrayAggSwitch[pod][0][indexsw1], arrayAggSwitch[pod][1][indexsw2])

		for pod in range (0, k):
			for switch in range (0, numAggSwitch):
				for hosts in range (0, numAggSwitch):
					self.addLink(arrayAggSwitch[pod][1][switch], arrayHosts[pod][switch][hosts])


topos = { 'mytopo': ( lambda: MyTopo() ) }

def myNet(controllers):
	tempTopo = MyTopo()
	net = Mininet( topo=tempTopo, controller=None)
	ctrl_count = 0
	for controllerIP in controllers:
		net.addController( 'c%d' % ctrl_count, RemoteController, ip=controllerIP)
        	ctrl_count += 1

	
	net.start()
	CLI(net)
	net.stop()

if __name__ == '__main__':
	setLogLevel( 'info' )
	if len(sys.argv) > 1:
		controllers = sys.argv[ 1: ]
	else:
		print 'Usage: sudopythtontree.py <c0 IP> <c1 IP> ...'
		exit(1)
	myNet(controllers)

	
