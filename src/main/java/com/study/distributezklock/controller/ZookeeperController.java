package com.study.distributezklock.controller;

import com.study.distributezklock.lock.ZkLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@RestController
@Slf4j
public class ZookeeperController {

    @Autowired
    private CuratorFramework client;

    @RequestMapping("zookeeperLock")
    public String zookeeperLock(){
        log.info("我进入了方法");
        try (ZkLock zkLock = new ZkLock()){
            if (zkLock.getLock("order")){
                log.info("我获得了锁！！");
                //可以写业务代码
                Thread.sleep(15000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("方法执行完成!");
        return "方法执行完成!";
    }

    @RequestMapping("curatorLock")
    public String curatorLock(){
        log.info("我进入了方法！");
        InterProcessMutex lock = new InterProcessMutex(client, "/order");
        try (ZkLock zkLock = new ZkLock()){
            if(lock.acquire(30, TimeUnit.SECONDS)){
                log.info("我获得了锁！！");
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                log.info("我释放了锁！！");
                lock.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        log.info("方法执行完成!");
        return "方法执行完成!";
    }

}
