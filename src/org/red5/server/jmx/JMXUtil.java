package org.red5.server.jmx;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.log4j.Logger;

/**
 * Helper methods for working with ObjectName or MBean instances.
 *
 * @author Paul Gregoire (mondain@gmail.com)
 */
public class JMXUtil {

	private static Logger log = Logger.getLogger(JMXUtil.class);

	public static void printMBeanInfo(ObjectName objectName, String className) {
		log.info("Retrieve the management information for the " + className);
		log.info("MBean using the getMBeanInfo() method of the MBeanServer");
		MBeanServer mbs = JMXFactory.getMBeanServer();
		MBeanInfo info = null;
		try {
			info = mbs.getMBeanInfo(objectName);
		} catch (Exception e) {
			log.error("Could not get MBeanInfo object for " + className
					+ " !!!", e);
			return;
		}
		log.info("CLASSNAME: \t" + info.getClassName());
		log.info("DESCRIPTION: \t" + info.getDescription());
		log.info("ATTRIBUTES");
		MBeanAttributeInfo[] attrInfo = info.getAttributes();
		if (attrInfo.length > 0) {
			for (int i = 0; i < attrInfo.length; i++) {
				log.info(" ** NAME: \t" + attrInfo[i].getName());
				log.info("    DESCR: \t" + attrInfo[i].getDescription());
				log.info("    TYPE: \t" + attrInfo[i].getType() + "\tREAD: "
						+ attrInfo[i].isReadable() + "\tWRITE: "
						+ attrInfo[i].isWritable());
			}
		} else
			log.info(" ** No attributes **");
		log.info("CONSTRUCTORS");
		MBeanConstructorInfo[] constrInfo = info.getConstructors();
		for (int i = 0; i < constrInfo.length; i++) {
			log.info(" ** NAME: \t" + constrInfo[i].getName());
			log.info("    DESCR: \t" + constrInfo[i].getDescription());
			log.info("    PARAM: \t" + constrInfo[i].getSignature().length
					+ " parameter(s)");
		}
		log.info("OPERATIONS");
		MBeanOperationInfo[] opInfo = info.getOperations();
		if (opInfo.length > 0) {
			for (int i = 0; i < opInfo.length; i++) {
				log.info(" ** NAME: \t" + opInfo[i].getName());
				log.info("    DESCR: \t" + opInfo[i].getDescription());
				log.info("    PARAM: \t" + opInfo[i].getSignature().length
						+ " parameter(s)");
			}
		} else
			log.info(" ** No operations ** ");
		log.info("NOTIFICATIONS");
		MBeanNotificationInfo[] notifInfo = info.getNotifications();
		if (notifInfo.length > 0) {
			for (int i = 0; i < notifInfo.length; i++) {
				log.info(" ** NAME: \t" + notifInfo[i].getName());
				log.info("    DESCR: \t" + notifInfo[i].getDescription());
				String notifTypes[] = notifInfo[i].getNotifTypes();
				for (int j = 0; j < notifTypes.length; j++) {
					log.info("    TYPE: \t" + notifTypes[j]);
				}
			}
		} else {
			log.info(" ** No notifications **");
		}
	}

}