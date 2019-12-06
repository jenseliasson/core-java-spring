package eu.arrowhead.core.datamanager.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.arrowhead.common.dto.shared.SenML;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.*; 
import java.util.Vector;

@Service
public class ProxyService {
	private static List<ProxyElement> endpoints = new ArrayList<>();

	static {
	endpoints = new ArrayList<>();
	
	}

	/**
	 * @fn static List<ProxyElement> getAllEndpoints()
	 *
	 */
	public static List<String> getAllEndpoints() {
		List<String> res = new ArrayList<>();
		Iterator<ProxyElement> epi = endpoints.iterator();

		while (epi.hasNext()) {
			ProxyElement pe = epi.next();
			res.add(pe.systemName);
		}
		return res;
	}

	/**
	 * @fn static List<ProxyElement> getEndpoints(String systemName)
	 *
	 */
	public static ArrayList<ProxyElement> getEndpoints(String systemName) {
		ArrayList<ProxyElement> res = new ArrayList<>();
		Iterator<ProxyElement> epi = endpoints.iterator();

		while (epi.hasNext()) {
			ProxyElement pe = epi.next();
			if (systemName.equals(pe.systemName)) {
				//System.out.println("Found endpoint: " + pe.serviceName);
				res.add(pe);
			}
		}
		return res;
	}

	/**
	 * @fn static boolean addEndpoint(ProxyElement e)
	 * @brief
	 *
	 */
	public static boolean addEndpoint(ProxyElement e) {
		for(ProxyElement tmp: endpoints) {
			if (tmp.serviceName.equals(e.serviceName)) // already exists
				return false;
		}
		endpoints.add(e);
		return true;
	}

	/**
	 * @fn static ProxyElement getEndpoint(String serviceName)
	 *
	 */
	public static ProxyElement getEndpoint(String serviceName) {
		Iterator<ProxyElement> epi = endpoints.iterator();

		while (epi.hasNext()) {
			ProxyElement curpe = epi.next();
			//System.out.println("Found endpoint: " + curpe.serviceName);
			if (serviceName.equals(curpe.serviceName)) {
				return curpe;
			}
		}

		return null;
	}

	/**
	 * @fn static boolean updateEndpoint(String serviceName, Vector<SenML> msg)
	 * @brief
	 *
	 */
	public static boolean updateEndpoint(String serviceName, Vector<SenML> msg) {
		Iterator<ProxyElement> epi = endpoints.iterator();

		while (epi.hasNext()) {
			ProxyElement pe = epi.next();
			if (serviceName.equals(pe.serviceName)) {
				//System.out.println("Found endpoint: " + pe.serviceName);
				pe.msg = msg; //.get(0);
				//System.out.println("Updating with: " + msg.toString());
				return true;
			}
		}
		return false;
	}
}
