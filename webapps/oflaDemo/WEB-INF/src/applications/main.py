"""
main.py - a translation into Python of the olfa demo Application class, a Red5 example.

@author The Red5 Project (red5@osflash.org)
@author Joachim Bauch (jojo@struktur.de)
"""

from org.red5.server.adapter import ApplicationAdapter
from org.red5.server.api.stream import IStreamCapableConnection
from org.red5.server.api.stream.support import SimpleBandwidthConfigure

class Application(ApplicationAdapter):
    
    def appStart(self, app):
        ApplicationAdapter.appStart(self, app)
        print 'Python appStart', app
        self.appScope = app
        return 1

    def appConnect(self, conn, params):
        ApplicationAdapter.appConnect(self, conn, params)
        print 'Python appConnect:', conn, params
        self.measureBandwidth(conn)
        if isinstance(conn, IStreamCapableConnection):
            print 'Python setting bandwidth limits'
            sbc = SimpleBandwidthConfigure()
            sbc.setMaxBurst(8388608)
            sbc.setBurst(8388608)
            sbc.setOverallBandwidth(2097152);
            conn.setBandwidthConfigure(sbc);
        
        return 1

    def toString(self):
        return 'Python:Application'

def getInstance():
    return Application()
