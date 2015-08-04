package com.boliao.sunshine.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.TagNameFilter;
import org.htmlparser.nodes.RemarkNode;
import org.htmlparser.nodes.TagNode;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.HeadingTag;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.Span;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;

import com.boliao.sunshine.biz.utils.LogUtil;

/**
 * ����html��ҳ��util
 * 
 * @author Liaobo
 * 
 */
public class ParseUtils {

	// ��־��¼��
	private static final Logger logger = Logger.getLogger(ParseUtils.class);
	// ������־��¼��
	private static final Logger errorLogger = Logger.getLogger(LogUtil.ERROR);

	/**
	 * ��ȡ����ĳ������ֵ�ı�ǩ�б�
	 * 
	 * @param <T>
	 * @param html
	 *            ����ȡ��HTML�ı�
	 * @param tagType
	 *            ��ǩ����
	 * @param attributeName
	 *            ĳ�����Ե�����
	 * @param attributeValue
	 *            ����Ӧȡ��ֵ
	 * @return
	 */
	public static <T extends TagNode> List<T> parseTags(String html, final Class<T> tagType, final String attributeName, final String attributeValue) {
		try {
			// ����һ��HTML������
			Parser parser = new Parser();
			parser.setInputHTML(html);

			NodeList tagList = parser.parse(new NodeFilter() {
				@Override
				public boolean accept(Node node) {

					if (node.getClass() == tagType) {
						T tn = (T) node;
						if (attributeName == null) {
							return true;
						}

						String attrValue = tn.getAttribute(attributeName);
						if (attrValue != null && attrValue.equals(attributeValue)) {
							return true;
						}
					}

					return false;
				}
			});

			List<T> tags = new ArrayList<T>();
			for (int i = 0; i < tagList.size(); i++) {
				T t = (T) tagList.elementAt(i);
				tags.add(t);
			}

			return tags;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T extends TagNode> List<T> parseTags(String html, final Class<T> tagType, final Map<String, String> atts) {
		try {
			// ����һ��HTML������
			Parser parser = new Parser();
			parser.setInputHTML(html);

			NodeList tagList = parser.parse(new NodeFilter() {
				@Override
				public boolean accept(Node node) {

					if (node.getClass() == tagType) {
						T tn = (T) node;
						if (atts == null) {
							return true;
						}
						for (String attributeName : atts.keySet()) {
							String attrValue = tn.getAttribute(attributeName);
							String value = atts.get(attributeName);
							if (attrValue == null || !attrValue.equals(value)) {
								return false;
							}
						}

						return true;
					}

					return false;
				}
			});

			List<T> tags = new ArrayList<T>();
			for (int i = 0; i < tagList.size(); i++) {
				T t = (T) tagList.elementAt(i);
				tags.add(t);
			}

			return tags;
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T extends TagNode> List<T> parseTags(String html, final Class<T> tagType) {
		return parseTags(html, tagType, null, null);
	}

	public static <T extends TagNode> T parseTag(String html, final Class<T> tagType, final String attributeName, final String attributeValue) {
		List<T> tags = parseTags(html, tagType, attributeName, attributeValue);
		if (tags != null && tags.size() > 0) {
			return tags.get(0);
		}
		return null;
	}

	public static <T extends TagNode> T parseTag(String html, final Class<T> tagType) {
		return parseTag(html, tagType, null, null);
	}

	/**
	 * �޸�HTML������������������ͼƬ�����ӵ�ַ������ָ����ǰ׺
	 * 
	 * @param html
	 *            Ҫ���޸ĵ�HTML����
	 * @param prefix
	 *            Ҫ���ӵ�ǰ׺
	 * @return ���޸�֮���HTML����
	 */
	public static String modifyImageUrl(String html, String prefix) {
		try {

			StringBuffer sb = new StringBuffer();

			// ����һ��HTML������
			Parser parser = new Parser();
			parser.setInputHTML(html);

			// nodeList�У���������ҳ�е���������
			NodeList nodeList = parser.parse(new NodeFilter() {
				public boolean accept(Node node) {
					return true;
				}
			});

			for (int i = 0; i < nodeList.size(); i++) {
				Node node = nodeList.elementAt(i);
				if (node instanceof ImageTag) {
					// �����<img>��ǩ
					ImageTag it = (ImageTag) node;
					it.setImageURL(prefix + it.getImageURL());
					sb.append(it.toHtml());
				} else if (node instanceof TextNode) { // �ı���ǩ��ԭ�����
					TextNode text = (TextNode) node;
					sb.append(text.getText());
				} else { // �������б�ǩ��ԭ�����
					sb.append("<");
					sb.append(node.getText());
					sb.append(">");
				}
			}

			return sb.toString();
		} catch (ParserException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ��������������ֵ��ȥ����Ӧ�ı�ǩ
	 * 
	 * @param <T>
	 * @param html
	 * @param tagType
	 * @param attributeName
	 * @param attributeValue
	 * @return
	 */
	public static <T extends TagNode> String reomveTags(String html, final Class<T> tagType, String attributeName, String attributeValue) {
		try {
			StringBuffer sb = new StringBuffer();
			Parser parser = new Parser();
			parser.setInputHTML(html);
			NodeList allNodes = parser.parse(new NodeFilter() {
				public boolean accept(Node node) {
					return true;
				}
			});

			for (int i = 0; i < allNodes.size(); i++) {
				Node node = allNodes.elementAt(i);
				if (node.getClass() == tagType) {
					TagNode tn = (TagNode) node;
					// ����Ƿ���Ҫ���tag�ڵ��ǩ
					if (StringUtils.equals(tn.getAttribute(attributeName), attributeValue)) {
						if (!tn.isEndTag()) { // ����һ����ʼ��ǩ
							removeTagNodeFromAllNodes(tn, allNodes);
							// while (sni.hasMoreNodes()) {
							// Node n = sni.nextNode();
							// allNodes.remove(n);
							// }
							i = i - 1;
						}
					} else { // ���������Ҫ��ԭ�����
						sb.append("<");
						sb.append(node.getText());
						sb.append(">");
					}
				} else if (node instanceof TextNode) {
					TextNode text = (TextNode) node;
					sb.append(text.getText());
				} else if (node instanceof RemarkNode) {
					RemarkNode rn = (RemarkNode) node;
					sb.append(rn.toHtml());
				} else {
					sb.append("<");
					sb.append(node.getText());
					sb.append(">");
				}
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * ������ţ�ɾ����Ӧ��TagNode
	 * 
	 * @param <T>
	 * @param html
	 * @param indexes
	 * @param tagType
	 * @return
	 */
	public static <T extends TagNode> String removeNodesByIndexes(String html, Set<Integer> indexes, final Class<T> tagType) {
		try {
			Parser parser = new Parser();
			parser.setInputHTML(html);
			NodeList allNodes = parser.parse(null);
			NodeList trs = allNodes.extractAllNodesThatMatch(new TagNameFilter("tr"), true);
			for (int i = 0; i < trs.size(); i++) {
				Node node = trs.elementAt(i);
				if (indexes.contains(i)) {
					removeTagNodeFromAllNodes((TagNode) node, allNodes);
					html = html.replace(node.toHtml(), "");
				}
			}
			return html;
		} catch (Exception e) {
			LogUtil.error(errorLogger, "ȥ������Ҫ�������еĽڵ�ʧ��", e);
		}
		return null;
	}

	/**
	 * ��allNodes���Ƴ�tn
	 * 
	 * @param tn
	 * @param allNodes
	 */
	private static void removeTagNodeFromAllNodes(TagNode tn, NodeList allNodes) {
		allNodes.remove(tn); // �Ƴ����ڵ�
		if (tn.getEndTag() != null)
			allNodes.remove(tn.getEndTag()); // �Ƴ����Ӧ�Ľ����ڵ�
		NodeList nl = tn.getChildren(); // �Ƴ�������������ӽڵ�
		if (nl != null) {
			SimpleNodeIterator sni = nl.elements();
			while (sni.hasMoreNodes()) {
				Node n = sni.nextNode();
				// allNodes.remove(n);
				removeAllChildrenTags(n, allNodes);
			}
		}
	}

	/**
	 * �ݹ�ɾ���ӽڵ�
	 * 
	 * @param node
	 * @param allNodes
	 */
	private static void removeAllChildrenTags(Node node, NodeList allNodes) {
		NodeList nl = node.getChildren(); // �Ƴ�������������ӽڵ�
		if (nl == null) {
			removeNode(node, allNodes);
			return;
		}

		SimpleNodeIterator sni = nl.elements();
		while (sni.hasMoreNodes()) {
			Node n = sni.nextNode();
			removeAllChildrenTags(n, allNodes);
			removeNode(node, allNodes);
		}
	}

	/**
	 * �Ƴ�node
	 * 
	 * @param node
	 * @param allNodes
	 */
	private static void removeNode(Node node, NodeList allNodes) {
		if (node instanceof TagNode) {
			TagNode nd = (TagNode) node;
			allNodes.remove(nd);
			allNodes.remove(nd.getEndTag());
		} else {
			allNodes.remove(node);
		}
	}

	public static void main(String[] args) {
		File file = new File("test");
		try {
			FileReader fr = new FileReader(file);
			BufferedReader br = new BufferedReader(fr);
			String line = br.readLine();
			StringBuilder sb = new StringBuilder();
			while (line != null) {
				sb.append(line);
				line = br.readLine();
			}
			fr.close();
			br.close();
			String result = reomveTags(sb.toString(), Span.class, "class", "score");
			result = ParseUtils.reomveTags(result, Div.class, "class", "ask_label");
			result = ParseUtils.reomveTags(result, Div.class, "class", "user_info fr");
			// �������µ�����
			List<HeadingTag> hs = ParseUtils.parseTags(result, HeadingTag.class, "class", "close");
			String linkTitile = (hs.get(0)).getChild(0).toHtml();
			String strTitle = (hs.get(0)).getChild(0).getFirstChild().toHtml();
			result = StringUtils.replace(result, linkTitile, strTitle);
			System.out.println(result);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}