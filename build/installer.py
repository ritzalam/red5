##
## Build Red5 and generate windows installer
##
##
import os, sys
import tempfile
from distutils.dir_util import remove_tree

# defaults
SVN_CMD  = r'C:\cygwin\bin\svn.exe'
ANT_CMD  = r'D:\Temp\apache-ant-1.6.5\bin\ant.bat'
BITROCK_CMD = r'C:\Programme\BitRock InstallBuilder Enterprise 3.5.1\bin\builder.exe'
VERSION  = '0.3'

REPOSITORIES = {
    #'red5_io': 'http://svn1.cvsdude.com/osflash/red5/java/io/trunk/',
    'red5_server': 'http://svn1.cvsdude.com/osflash/red5/java/server/tags/0.3/',
}

def error(msg):
    print 'ERROR: %s' % msg
    sys.exit(1)

def log(msg):
    print msg


class Builder:
    
    def compile(self, ant, script, *args):
        args = args and ' ' + ' '.join(args) or ''
        assert os.system('%s -quiet -buildfile %s%s' % (ant, script, args)) == 0

    def __init__(self, java_home, ant_cmd, workdir=None):
        if workdir is None:
            workdir = tempfile.mkdtemp('red5')
        self.workdir = workdir
        self.java_home = java_home
        self.ant_cmd = ant_cmd
        self.build_root = os.path.abspath(os.path.split(__file__)[0])

    def checkout(self, url, path):
        log('Checking out of %s' % url)
        assert os.system("%s export %s %s" % (SVN_CMD, url, path)) == 0

    def build(self, platform='windows'):
        log('Using %s as temporary directory' % self.workdir)
        # checkout Red5 from repository
        for path, url in REPOSITORIES.items():
            self.checkout(url, os.path.join(self.workdir, path))
        
        # generate red5.jar
        log('Building red5.jar')
        red5_root = os.path.join(self.workdir, 'red5_server')
        self.compile(self.ant_cmd, os.path.join(red5_root, 'build.xml'), 'jar')
        
        # build installer
        dest = os.getcwd()
        script = os.path.join(self.build_root, 'red5_bitrock.xml')
        cmd = BITROCK_CMD
        try:
            import win32api
        except ImportError:
            pass
        else:
            cmd = win32api.GetShortPathName(cmd)
        log('Compiling installer, this may take some time...')
        assert os.system('%s build %s %s' % (cmd, script, platform)) == 0
        log('Installer written to %s\setup-red5-%s-%s.*' % (dest, VERSION, platform))
        
        # cleanup
        remove_tree(self.workdir)


def main():
    log('Red5 build system')
    log('-----------------')
    JAVA_HOME = os.environ.get('JAVA_HOME', r'C:\Programme\Java\jdk1.5.0_05')
    
    if not os.path.isfile(os.path.join(JAVA_HOME, 'bin', 'java.exe')):
        error('could not find "java.exe" in "%s"' % JAVA_HOME)
        
    log('using "java.exe" from "%s"' % JAVA_HOME)
    os.environ['JAVACMD'] = os.path.join(JAVA_HOME, 'bin', 'java.exe')
    
    if not os.path.isfile(ANT_CMD):
        error('"%s" does not exist' % ANT_CMD)
        
    log('using "%s" for building' % ANT_CMD)
    
    #builder = Builder(JAVA_HOME, ANT_CMD, workdir=r'c:\dokume~1\magog\lokale~1\temp\tmpr3bb79red5')
    builder = Builder(JAVA_HOME, ANT_CMD)
    builder.build()
    

if __name__ == '__main__':
    main()
