package com.alibaba.dubbo.rpc.protocol.dubbo;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.RemotingException;
import com.alibaba.dubbo.remoting.TimeoutException;
import com.alibaba.dubbo.remoting.exchange.ExchangeClient;
import com.alibaba.dubbo.remoting.exchange.support.header.HeaderExchangeClient;
import com.alibaba.dubbo.remoting.transport.ClientDelegate;
import com.alibaba.dubbo.rpc.*;
import com.alibaba.dubbo.rpc.protocol.AbstractInvoker;

import java.net.InetSocketAddress;

/**
 * 基于已有channel的invoker.
 *
 * @author chao.liuc
 */
class ChannelWrappedInvoker<T> extends AbstractInvoker<T> {

    private final Channel channel;
    private final String serviceKey;

    public ChannelWrappedInvoker(Class<T> serviceType, Channel channel, URL url, String serviceKey) {

        super(serviceType, url, new String[]{Constants.GROUP_KEY,
                Constants.TOKEN_KEY, Constants.TIMEOUT_KEY});
        this.channel = channel;
        this.serviceKey = serviceKey;
    }

    @Override
    protected Result doInvoke(Invocation invocation) throws Throwable {
        RpcInvocation inv = (RpcInvocation) invocation;
        //拿不到client端export 的service path.约定为interface的名称.
        inv.setAttachment(Constants.PATH_KEY, getInterface().getName());
        inv.setAttachment(Constants.CALLBACK_SERVICE_KEY, serviceKey);

        ExchangeClient currentClient = new HeaderExchangeClient(new ChannelWrapper(this.channel));

        try {
            if (getUrl().getMethodParameter(invocation.getMethodName(), Constants.ASYNC_KEY, false)) { // 不可靠异步
                currentClient.send(inv, getUrl().getMethodParameter(invocation.getMethodName(), Constants.SENT_KEY, false));
                return new RpcResult();
            }
            int timeout = getUrl().getMethodParameter(invocation.getMethodName(),
                    Constants.TIMEOUT_KEY, Constants.DEFAULT_TIMEOUT);
            return timeout > 0 ? (Result) currentClient.request(inv, timeout).get() : (Result) currentClient.request(inv).get();
        } catch (RpcException e) {
            throw e;
        } catch (TimeoutException e) {
            throw new RpcException(RpcException.TIMEOUT_EXCEPTION, e.getMessage(), e);
        } catch (RemotingException e) {
            throw new RpcException(RpcException.NETWORK_EXCEPTION, e.getMessage(), e);
        } catch (Throwable e) { // here is non-biz exception, wrap it.
            throw new RpcException(e.getMessage(), e);
        }
    }

    public static class ChannelWrapper extends ClientDelegate {

        private final Channel channel;
        private final URL url;

        public ChannelWrapper(Channel channel) {
            this.channel = channel;
            this.url = channel.getUrl().addParameter("codec", DubboCodec.NAME);
        }

        public URL getUrl() {
            return url;
        }

        public ChannelHandler getChannelHandler() {
            return channel.getChannelHandler();
        }

        public InetSocketAddress getLocalAddress() {
            return channel.getLocalAddress();
        }

        public void close() {
            channel.close();
        }

        public boolean isClosed() {
            return channel == null || channel.isClosed();
        }

        public void reset(URL url) {
            throw new RpcException("ChannelInvoker can not reset.");
        }

        public InetSocketAddress getRemoteAddress() {
            return channel.getLocalAddress();
        }

        public boolean isConnected() {
            return channel != null && channel.isConnected();
        }

        public boolean hasAttribute(String key) {
            return channel.hasAttribute(key);
        }

        public Object getAttribute(String key) {
            return channel.getAttribute(key);
        }

        public void setAttribute(String key, Object value) {
            channel.setAttribute(key, value);
        }

        public void removeAttribute(String key) {
            channel.removeAttribute(key);
        }

        public void reconnect() throws RemotingException {

        }

        public void send(Object message) throws RemotingException {
            channel.send(message);
        }

        public void send(Object message, boolean sent) throws RemotingException {
            channel.send(message, sent);
        }

    }

    public void destroy() {
        //channel资源的清空由channel创建者清除.
//        super.destroy();
//        try {
//            channel.close();
//        } catch (Throwable t) {
//            logger.warn(t.getMessage(), t);
//        }
    }

}