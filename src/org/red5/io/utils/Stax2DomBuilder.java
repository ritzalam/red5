/*--

 Copyright (C) 2005 Tatu Saloranta
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions, and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions, and the disclaimer that follows
    these conditions in the documentation and/or other materials
    provided with the distribution.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED.  IN NO EVENT SHALL THE JDOM AUTHORS OR THE PROJECT
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 SUCH DAMAGE.

 */

package org.red5.io.utils;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Builds a DOM {@link org.w3c.dom.Document} using a
 * {@link javax.xml.stream.XMLStreamReader}.
 * 
 * @version $Revision: 1.00 $, $Date: 2004/12/11 00:00:00 $
 * @author Tatu Saloranta
 */
public class Stax2DomBuilder {
	// // // Configuration settings:

	/**
	 * Whether ignorable white space should be ignored, ie not added in the
	 * resulting JDOM tree. If true, it will be ignored; if false, it will be
	 * added in the tree. Default value if false.
	 */
	protected boolean mCfgIgnoreWs = false;

	protected boolean mNsAware = true;

	// // Trivial caching...

	protected String mLastPrefix = null;

	protected String mLastLocalName = null;

	protected String mLastQName = null;

	/**
	 * Default constructor.
	 */
	public Stax2DomBuilder() {

	}

	/**
	 * Method used to change whether the build methods will add ignorable
	 * (element) white space in the DOM tree or not.
	 *<p>
	 * Whether all-whitespace text segment is ignorable white space or not is
	 * based on DTD read in, as per XML specifications (white space is only
	 * significant in mixed content or pure text elements).
	 */
	public void setIgnoreWhitespace(boolean state) {
		mCfgIgnoreWs = state;
	}

	/**
	 * This method will create a {@link org.w3c.dom.Document} instance using the
	 * default JAXP mechanism and populate using the given StAX stream reader.
	 * 
	 * @param r
	 *            Stream reader from which input is read.
	 * @return <code>Document</code> - DOM document object.
	 * @throws XMLStreamException
	 *             If the reader threw such exception (to indicate a parsing or
	 *             I/O problem)
	 */
	public Document build(XMLStreamReader r)
			throws ParserConfigurationException, XMLStreamException {
		return build(r, DocumentBuilderFactory.newInstance()
				.newDocumentBuilder());
	}

	public Document build(XMLStreamReader r, DocumentBuilder docbuilder)
			throws XMLStreamException {
		Document doc = docbuilder.newDocument();
		build(r, doc);
		return doc;
	}

	/**
	 * This method will populate given {@link org.w3c.dom.Document} using the
	 * given StAX stream reader instance.
	 * 
	 * @param r
	 *            Stream reader from which input is read.
	 * @return <code>Document</code> - DOM document object.
	 * @throws XMLStreamException
	 *             If the reader threw such exception (to indicate a parsing or
	 *             I/O problem)
	 */
	public void build(XMLStreamReader r, Document doc)
			throws XMLStreamException {
		buildTree(r, doc);
	}

	/**
	 * This method takes a <code>XMLStreamReader</code> and builds up a JDOM
	 * tree. Recursion has been eliminated by using nodes' parent/child
	 * relationship; this improves performance somewhat (classic
	 * recursion-by-iteration-and-explicit stack transformation)
	 * 
	 * @param f
	 *            Node factory to use for creating JDOM nodes
	 * @param r
	 *            Stream reader to use for reading the document from which to
	 *            build the tree
	 * @param doc
	 *            JDOM <code>Document</code> being built.
	 * @param tmod
	 *            Text modifier to use for modifying content of text nodes
	 *            (CHARACTERS, not CDATA), if any; null if no modifications are
	 *            needed (modifier is usually used for trimming unnecessary but
	 *            non-ignorable white space).
	 */
	protected void buildTree(XMLStreamReader r, Document doc)
			throws XMLStreamException {
		checkReaderSettings(r);

		Node current = doc; // At top level

		main_loop:

		while (true) {
			int evtType = r.next();
			Node child;

			switch (evtType) {
				case XMLStreamConstants.CDATA:
					child = doc.createCDATASection(r.getText());
					break;

				case XMLStreamConstants.SPACE:
					if (mCfgIgnoreWs) {
						continue main_loop;
					}
					/*
					 * Oh great. DOM is brain-dead in that ignorable white space
					 * can not be added, even though it is legal, and often
					 * reported by StAX/SAX impls...
					 */
					if (current == doc) { // better just ignore, thus...
						continue;
					}
					// fall through

				case XMLStreamConstants.CHARACTERS:
					child = doc.createTextNode(r.getText());
					break;

				case XMLStreamConstants.COMMENT:
					child = doc.createComment(r.getText());
					break;

				case XMLStreamConstants.END_DOCUMENT:
					break main_loop;

				case XMLStreamConstants.END_ELEMENT:
					current = current.getParentNode();
					if (current == null) {
						current = doc;
					}
					continue main_loop;

				case XMLStreamConstants.ENTITY_DECLARATION:
				case XMLStreamConstants.NOTATION_DECLARATION:
					/*
					 * Shouldn't really get these, but maybe some stream readers
					 * do provide the info. If so, better ignore it -- DTD event
					 * should have most/all we need.
					 */
					continue main_loop;

				case XMLStreamConstants.ENTITY_REFERENCE:
					child = doc.createEntityReference(r.getLocalName());
					break;

				case XMLStreamConstants.PROCESSING_INSTRUCTION:
					child = doc.createProcessingInstruction(r.getPITarget(), r
							.getPIData());
					break;

				case XMLStreamConstants.START_ELEMENT:
					// Ok, need to add a new element...
				{
					String ln = r.getLocalName();
					Element newElem;

					if (mNsAware) {
						String elemPrefix = r.getPrefix();

						// Doh, DOM requires a silly qualified name...
						if (elemPrefix != null && elemPrefix.length() > 0) {
							newElem = doc.createElementNS(r.getNamespaceURI(),
									getQualified(elemPrefix, ln));
						} else {
							newElem = doc.createElementNS(r.getNamespaceURI(),
									ln);
						}
					} else { // if non-ns-aware, things are simpler:
						newElem = doc.createElement(ln);
					}

					/*
					 * No need to check namespace bindings, unlikes with some
					 * other frameworks (JDOM)
					 */

					// And then the attributes:
					for (int i = 0, len = r.getAttributeCount(); i < len; ++i) {
						ln = r.getAttributeLocalName(i);
						if (mNsAware) {
							String prefix = r.getAttributePrefix(i);
							if (prefix != null && prefix.length() > 0) {
								ln = getQualified(prefix, ln);
							}
							Attr attr = doc.createAttributeNS(r
									.getAttributeNamespace(i), ln);
							attr.setValue(r.getAttributeValue(i));
							newElem.setAttributeNodeNS(attr);
						} else {
							Attr attr = doc.createAttribute(ln);
							attr.setValue(r.getAttributeValue(i));
							newElem.setAttributeNode(attr);
						}
					}
					// And then 'push' new element...
					current.appendChild(newElem);
					current = newElem;
					continue main_loop;
				}

				case XMLStreamConstants.START_DOCUMENT:
					/*
					 * This should only be received at the beginning of
					 * document... so, should we indicate the problem or not?
					 */
					/*
					 * For now, let it pass: maybe some (broken) readers pass
					 * that info as first event in beginning of doc?
					 */
					continue main_loop;

				case XMLStreamConstants.DTD:
					/*
					 * !!! Note: StAX does not expose enough information about
					 * doctype declaration (specifically, public and system
					 * id!); (altough StAX2 would...)
					 * 
					 * Worse, DOM1/2 do not specify a way to create the DocType
					 * node, even if StAX provided it. This is pretty silly, all
					 * in all.
					 */
					continue main_loop;

					// Should never get these, from a stream reader:

					/*
					 * (commented out entries are just FYI; default catches them
					 * all)
					 */

					// case XMLStreamConstants.ATTRIBUTE:
					// case XMLStreamConstants.NAMESPACE:
				default:
					throw new XMLStreamException(
							"Unrecognized iterator event type: "
									+ r.getEventType()
									+ "; should not receive such types (broken stream reader?)");
			}

			if (child != null) {
				current.appendChild(child);
			}
		}
	}

	// // // Overridable helper methods:

	protected String getQualified(String prefix, String localName) {
		/*
		 * This mostly/only helps with empty/text-only elements... might make
		 * sense to do 'real' caching...
		 */
		if (localName == mLastLocalName && prefix == mLastPrefix) {
			return mLastQName;
		}
		String qn = prefix + ":" + localName;
		mLastQName = qn;
		return qn;
	}

	protected void checkReaderSettings(XMLStreamReader r)
			throws XMLStreamException {
		Object o = r.getProperty(XMLInputFactory.IS_NAMESPACE_AWARE);
		/*
		 * StAX defaults to namespace aware, so let's use similar logics
		 * (although all compliant implementations really should return a valid
		 * value)
		 */
		if ((o instanceof Boolean) && !((Boolean) o).booleanValue()) {
			mNsAware = false;
		} else {
			mNsAware = true;
		}
	}

}
