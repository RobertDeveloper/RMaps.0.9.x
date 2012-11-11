package com.robert.maps.kml.XMLparser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlSerializer;

import android.util.Xml;

import com.robert.maps.utils.Ut;

public class SimpleXML {

	  private SimpleXML fparent;
	  private String ftext;
	  private String fname;
	  private Vector<SimpleXML> fchild;
	  private HashMap<String, String> fattrs;

	  public String getText() {
	    return ftext;
	  }

	  public void setText(String newText) {
	    ftext = newText;
	  }

	  public String getAttr(String attrName) {
	    if (fattrs.containsKey(attrName)) return fattrs.get(attrName);
	    return "";
	  }

	  public void setAttr(String attrName, String attrValue) {
	    fattrs.put(attrName, attrValue);
	  }

	  public int getAttrCount() {
	    return fattrs.size();
	  }

	  public int getChildCount() {
	    return fchild.size();
	  }

	  public Vector<SimpleXML> getChildren() {
	    return fchild;
	  }

	  public SimpleXML getParent() {
	    return fparent;
	  }

	  public void setParent(SimpleXML newParent) {
	    if (fparent != null) {
	      try {
	        fparent.fchild.remove(this);
	      } catch (Exception e) { }
	    }
	    if (newParent instanceof SimpleXML) {
	      fparent = newParent;
	      try {
	        fparent.fchild.add(this);
	      } catch (Exception e) { }
	    } else {
	      fparent = null;
	    }
	  }

	  public SimpleXML(String nodeName) {
	    fname = nodeName;
	    ftext = "";
	    fattrs = new HashMap<String, String>();
	    fchild = new Vector<SimpleXML>();
	    fparent = null;
	  }

	  public static SimpleXML fromNode(Node node) {
	    SimpleXML ret = null;
	    if (node != null) {
	      try {
	        ret = new SimpleXML(node.getNodeName());

	        if (node.hasAttributes()) {
	          NamedNodeMap nattr = node.getAttributes();
	          for(int f = 0; f < nattr.getLength(); ++f) {
	            ret.setAttr(nattr.item(f).getNodeName(), nattr.item(f).getNodeValue());
	          }
	        }

	        if (node.hasChildNodes()) {
	          NodeList nlc = node.getChildNodes();

	          for(int f = 0; f < nlc.getLength(); ++f) {
	            if (nlc.item(f).getNodeType() == Node.TEXT_NODE) {
	              ret.ftext += nlc.item(f).getNodeValue();
	            } else if (nlc.item(f).getNodeType() == Node.ENTITY_REFERENCE_NODE) {
	              String nv = nlc.item(f).getNodeName();
	              if (nv != null && nv.length() > 1 && nv.startsWith("#")) {
	                nv = nv.substring(1);
	                try {
	                  int[] z =  { Integer.parseInt(nv) };
	                  String s = new String(z, 0, z.length);
	                  ret.ftext += s;
	                } catch (Exception e) { }
	              }
	            } else {
	              SimpleXML rchild = SimpleXML.fromNode(nlc.item(f));
	              if (rchild != null) ret.getChildren().add(rchild);
	            }
	          }
	        }

	      } catch (Exception e) { }
	    }
	    return ret;
	  }

	  public SimpleXML createChild(String nodeName) {
	    SimpleXML child = new SimpleXML(nodeName);
	    child.setParent(this);
	    return child;
	  }

	  public Vector<SimpleXML> getChildren(String nodeName) {
	    Vector<SimpleXML> ret = new Vector<SimpleXML>();
	    try {
	      Iterator<SimpleXML> i = fchild.iterator();
	      while(i.hasNext()) {
	        SimpleXML xml = i.next();
	        if (xml.fname.equalsIgnoreCase(nodeName)) {
	          ret.add(xml);
	        }
	      }
	    } catch (Exception e) { }

	    return ret;
	  }

	  public SimpleXML getNodeByPath(String nodePath, boolean createIfNotExists) {

	    if (nodePath == null || nodePath.trim().equalsIgnoreCase("")) return null;

	    SimpleXML ret = null;

	    try {
	      String[] bpath = nodePath.split("\\\\");
	      if (bpath != null && bpath.length > 0) {
	        int scnt = 0;
	        for(int f = 0; f < bpath.length; ++f) {
	          String c = bpath[f].trim();
	          if (c != null && c.length() > 0) scnt++;
	        }

	        if (scnt > 0) {
	          ret = this;
	          for(int f = 0; f < bpath.length; ++f) {
	            String c = bpath[f].trim();
	            if (c != null && c.length() > 0) {
	              Vector<SimpleXML> curnodes = ret.getChildren(c);
	              if (curnodes != null && curnodes.size() > 0) {
	                ret = curnodes.firstElement();
	              } else {
	                if (createIfNotExists) {
	                  ret = ret.createChild(c);
	                } else {
	                  ret = null;
	                  break;
	                }
	              }
	            }
	          }
	        }
	      }
	    } catch (Exception e) {
	    }

	    return ret;
	  }

	  public static SimpleXML loadXml(String txxml) {
	    SimpleXML ret = null;
	    try {
	      ret = SimpleXML.loadXml(new ByteArrayInputStream(txxml.getBytes()));
	    } catch (Exception e) { }

	    return ret;
	  }

	  public static SimpleXML loadXml(InputStream isxml) {
	    SimpleXML ret = null;
	    try {
	      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
	      DocumentBuilder dbBuilder = dbFactory.newDocumentBuilder();

	      Document doc = dbBuilder.parse(isxml);
	      ret = SimpleXML.fromNode(doc.getDocumentElement());
	    } catch (Exception e) { }

	    return ret;
	  }

	  void serializeNode(XmlSerializer ser) {
	    try {
	      ser.startTag("", fname);
	      for(Entry<String, String> ee : fattrs.entrySet()) {
	        ser.attribute("", ee.getKey(), ee.getValue());
	      }

	      if (fchild.size() > 0) {
				for (SimpleXML c : fchild) {
					c.serializeNode(ser);
				}
			} else {
				if (ftext != null)
					ser.text(ftext);
			}
	      ser.endTag("", fname);
	    } catch(Exception e) {
	      Ut.d("e: " + e.toString());
	    }
	  }

	  public static String saveXml(SimpleXML document) {
	    try {
	      ByteArrayOutputStream baos = new ByteArrayOutputStream();
	      XmlSerializer xs = Xml.newSerializer();
	      xs.setOutput(baos, "Utf-8");
	      xs.startDocument("Utf-8", true);
	      document.serializeNode(xs);
	      xs.endDocument();
	      return new String(baos.toByteArray());
	    } catch(Exception e) { }

	    return "";
	  }
}
