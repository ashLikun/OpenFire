package com.qiuzhping.openfire.plugin;


import org.dom4j.Element;
import org.jivesoftware.openfire.RoutingTable;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.openfire.user.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

import java.io.File;
import java.util.List;

/**
 * 作者　　: 李坤
 * 创建时间: 2018/12/28　15:57
 * 邮箱　　：496546144@qq.com
 * <p>
 * 功能介绍：多消息发送
 */

public class MultiMsg implements PacketInterceptor, Plugin {
    private static final Logger log = LoggerFactory.getLogger(MultiMsg.class);
    private RoutingTable routingTable;
    InterceptorManager interceptorManager;

    public void debug(String str) {
        log.debug(str);
    }

    @Override
    public void initializePlugin(PluginManager pluginManager, File file) {
        interceptorManager = InterceptorManager.getInstance();
        interceptorManager.addInterceptor(this);
        XMPPServer server = XMPPServer.getInstance();
        routingTable = server.getRoutingTable();
    }

    @Override
    public void interceptPacket(Packet packet, Session session, boolean incoming, boolean processed) {
        try {
            JID recipient = packet.getTo();
            if (recipient != null) {
                String username = recipient.getNode();
                // if broadcast message or user is not exist
                if (username == null || !UserManager.getInstance().isRegisteredUser(recipient)) {
                    return;
                } else if (!XMPPServer.getInstance().getServerInfo().getXMPPDomain().equals(recipient.getDomain())) {
                    return;
                }
            }
            // incoming表示本条消息刚进入openfire。processed为false，表示本条消息没有被openfire处理过。这说明这是一条处女消息，也就是没有被处理过的消息。
            if (processed || !incoming) {
                return;
            }
            doAction(packet, session);
        } catch (Exception e) {
            e.printStackTrace();
            debug("EEEEEEE----- error interceptPacket " + e.toString());
        }
    }

    private void doAction(Packet packet, Session session) {
        Packet copyPacket = packet.createCopy();
        if (packet instanceof Message) {
            Message message = (Message) copyPacket;
            if (message.getType() == Message.Type.chat) {
                debug("EEEEEEE----- multiMsg 33333");
                Message sendmessage = (Message) packet;
                String content = sendmessage.getBody();
                if (content != null) {
                    //如果当前用户需要把消息发送给其他人
                    List<Element> subject = message.getElement().elements("subject");
                    if (subject != null && !subject.isEmpty()) {
                        Message multiMsg = message.createCopy();
                        JID from = new JID(multiMsg.getFrom().toBareJID());
                        JID oldTo = multiMsg.getTo();
                        String fromResource = multiMsg.getFrom().getResource();
                        multiMsg.setTo(from);
                        for (Element sub : subject) {
                            String lang = sub.attributeValue("xml:lang");
                            debug("EEEEEEE----- multiMsg sub  =  " + sub.toString());
                            debug("EEEEEEE----- multiMsg sub  =  " + sub.asXML());
                            debug("EEEEEEE----- multiMsg sub  =  " + sub.getQualifiedName());
                            debug("EEEEEEE----- multiMsg sub  =  " + sub.getStringValue());
                            debug("EEEEEEE----- multiMsg sub  =  " + sub.getNodeTypeName());
                            debug("EEEEEEE----- multiMsg sub2  =  " + sub.getQName().getQualifiedName());
                            debug("EEEEEEE----- multiMsg sub2  =  " + sub.getQName().getNamespacePrefix());
                            debug("EEEEEEE----- multiMsg sub2  =  " + sub.getQName().getName());
                            debug("EEEEEEE----- multiMsg lang  =  " + lang);
                            if (lang != null && lang.equals("oneself")) {
                                sub.setText(oldTo.toFullJID());
                                debug("EEEEEEE----- multiMsg success  =  " + multiMsg.toXML());
                                //如果 <subject xml:lang="MultiMsg">aaaaa</subject> 就代表要转发
                                for (JID route : routingTable.getRoutes(from, null)) {
                                    if (fromResource == null || fromResource != route.getResource()) {
                                        debug("EEEEEEE----- multiMsg routePacket" + route.getNode());
                                        routingTable.routePacket(route, multiMsg, false);
                                    }
                                }
                            }
                        }
                    }


                }
            }
        }

    }

    @Override
    public void destroyPlugin() {
        interceptorManager.removeInterceptor(this);
    }
}
