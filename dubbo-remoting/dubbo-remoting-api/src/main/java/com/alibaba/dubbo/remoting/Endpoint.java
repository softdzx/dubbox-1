package com.alibaba.dubbo.remoting;

import java.net.InetSocketAddress;

import com.alibaba.dubbo.common.URL;

/**
 * Endpoint. (API/SPI, Prototype, ThreadSafe)
 * 
 * @see com.alibaba.dubbo.remoting.Channel
 * @see com.alibaba.dubbo.remoting.Client
 * @see com.alibaba.dubbo.remoting.Server
 * @author william.liangf
 */
public interface Endpoint {

    /**
     * get url.
     * 
     * @return url
     */
    URL getUrl();

    /**
     * get channel handler.
     * 
     * @return channel handler
     */
    ChannelHandler getChannelHandler();

    /**
     * get local address.
     * 
     * @return local address.
     */
    InetSocketAddress getLocalAddress();
    
    /**
     * send message.
     * 
     * @param message message
     * @throws RemotingException remote exception
     */
    void send(Object message) throws RemotingException;

    /**
     * send message.
     * 
     * @param message message
     * @param sent 是否已发送完成
     */
    void send(Object message, boolean sent) throws RemotingException;

    /**
     * close the channel.
     */
    void close();
    
    /**
     * Graceful close the channel.
     */
    void close(int timeout);
    
    /**
     * is closed.
     * 
     * @return closed
     */
    boolean isClosed();

}