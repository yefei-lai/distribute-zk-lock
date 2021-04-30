package com.study.distributezklock.lock;

import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class ZkLock implements AutoCloseable, Watcher {

    private ZooKeeper zooKeeper;

    private String znode;

    public ZkLock() throws IOException {
        this.zooKeeper = new ZooKeeper("localhost:2181",10000,this);
    }

    public boolean getLock(String businessCode){
        try {
            //创建业务根节点-持久类型 false不监听
            Stat stat = zooKeeper.exists("/"+businessCode, false);
            if (stat == null){
                zooKeeper.create("/"+businessCode, businessCode.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }

            //创建瞬时有序节点 /order/order_000001
            znode = zooKeeper.create("/"+businessCode+"/"+businessCode+"_",
                    businessCode.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
            //获取业务节点下所有的子节点
            List<String> childrenNodes = zooKeeper.getChildren("/"+businessCode,false);
            //子节点排序
            Collections.sort(childrenNodes);
            //获取序号最小的（第一个）子节点
            String firstNode = childrenNodes.get(0);
            //如果创建的节点是第一个子节点，则获得锁
            if (znode.endsWith(firstNode)){
                return true;
            }
            //不是第一个子节点，则监听前一个节点
            String lastNode = firstNode;
            for (String node : childrenNodes){
                if (znode.endsWith(node)){
                    zooKeeper.exists("/"+businessCode+"/"+lastNode,true);
                    break;
                }else {
                    lastNode = node;
                }
            }
            //synchronized加锁，其他未获取锁的线程顺序等待被唤醒
            synchronized(this){
                wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public void close() throws Exception {
        zooKeeper.delete(znode, -1);
        zooKeeper.close();
        log.info("我已经释放了锁！");
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        //观察事件类型 - 前一个节点删除后唤醒排队等待的线程
        if (watchedEvent.getType() == Event.EventType.NodeDeleted){
            synchronized(this){
                notify();
            }
        }
    }
}
