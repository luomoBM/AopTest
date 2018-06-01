---
title: Android-AOP-利器-Javassist
date: 2018-05-30 17:00:16
categories: 
- AOP
---

AOP简介
============

### 什么是 AOP

面向切面编程, AOP,  Aspect Oriented Program 的首字母缩写。  面向对象（OOP）的特点是把功能分配到不同的对象中， 软件设计中称为指责分配。 

假象下，每个对象都可以看做是种个圆柱体，当每个圆柱体都需要做一件有共性事情（加个粉色的圆，突出圆柱）的时候。如果用面向对象的思想，只能每个面都是修改下，而且重复 cv 的修改。
 
上面问题中有一个大刀， 能一次把所有圆柱切开，一次把所有面都加工好。那么，这个行为就是AOP。可以这么理解，**AOP是OOP的横向补充，把每个原本只有二维的面切开，拉伸出第三维度（Z轴）来**。

### AOP 能干什么

android 中比较常用的如
- 统一添加调用时长统计，日志统计，log输出等
- 部分页面是需要验证登录通过才能打开
- 统一添加权限申请验证
- 按钮防重复点击

等等诸如此类。

### AOP 如何实现

传统的实现方式是通过代理模式，这种方式有一定的局限。如果可以通过编译器编译或脚本自动插入代码，这种方式是比较优雅，可拓展性强的。

这就要介绍下 android 的 AOP 三剑客了。APT、AspectJ、Javassist对应编译时期。

<!-- more -->
![APT,AspectJ,Javassist对应编译时期](http://p9iqqot9p.bkt.clouddn.com/AOP%E5%AE%9E%E7%8E%B0%E5%9B%BE.jpg)

今天我要讲的就是这个 Javassist 了。在这之前，先给说下 gradle 插件。

Gradle 插件开发
================

`apply plugin: 'com.android.application'`
`apply plugin: 'com.android.library'`
这两行当代相信大家再熟悉不过了。这两个其实也都是 gradle 插件，一个是可运行的 app module ，一个是依赖库。

这是 gradle 自带的插件，其他也有很多，像我们比较多使用的如 ButterKnife、Lambda 这些常用的也是大牛封装好的在线插件。接下来我们要开发我们自己的插件。

我们新建一个 `plugin` module 中 `build.gralde` 配置一下信息
``` gradle
apply plugin: 'groovy'
apply plugin: 'maven'

dependencies {
    compile gradleApi()//gradle sdk
    compile localGroovy()//groovy sdk
    compile 'com.android.tools.build:gradle:3.1.2'
    //compile 'org.javassist:javassist:3.20.0-GA'
}

repositories {
    mavenCentral()
}

uploadArchives {//一个task
    repositories.mavenDeployer {
        repository(url: uri('../repo'))
        pom.groupId = 'com.app.plugin'
        pom.artifactId = 'gradleplugin'
        pom.version = '1.0.0'
    }
}
```

在 `main`目录下新建一个`groovy`和`resources`目录， 这个是固定的格式。如图，上面的`groovy`下的包名结构可自定义。 
![gradle 插件开发目录结构](http://p9iqqot9p.bkt.clouddn.com/%E6%8F%92%E4%BB%B6%E5%BC%80%E5%8F%91%E7%9B%AE%E5%BD%95%E7%BB%93%E6%9E%84.png)

plugin 类实现 `Plugin<Project>`接口
``` groovy
package com.app.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by crizquan on 2018/5/31.
 * <p>
 * desc:
 * */
class JavassistPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {

    }
}
```
**com.app.plugin.javassist.properties**只有一行
`implementation-class=com.app.plugin.JavassistPlugin`写上JavassistPlugin 的全路径

然后执行`gradlew -p splugin uploadArchives`或者在右边栏找到`uploadArchives`命令执行。就会生成我们的插件，出现这个就带表我们生成的插件成功了。**每次修改后都要执行这个命令更新插件**

![生成插件目录](http://p9iqqot9p.bkt.clouddn.com/%E7%94%9F%E6%88%90%E6%8F%92%E4%BB%B6%E7%9B%AE%E5%BD%95.png)

**project 的 build.gradle**下需要配置一下插件应用

``` gradle
buildscript {
    
    repositories {
        google()
        jcenter()
        maven{
            url uri('repo')
        }
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:3.1.2'
        classpath 'com.app.plugin:gradleplugin:1.0.0'

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}
```

在 module `build.gradle`中应用下插件，就完成了

``` gradle
import com.app.plugin.JavassistPlugin

apply plugin: 'com.android.application'
apply plugin: JavassistPlugin
```


Javassist 实现事件分发
================

类似 EventBus 实现，通过注解的方式， 完成事件分发。因为纵观 EventBus 还是有相当一部分是通过异端分子（反射）来实现的，性能有约束。今天来试试用 Javassist 完美实现 EventBus 事件分发。

> 写这个是因为目前项目也没有用 EventBus ，是用自己封装的类，用的 `Map<IEventHandler>, List<IEventHandler>>`
 三个这样的 Map，-个存 handler 、一个存 handler 的自定义方法、一个存锁引用， 有个好处就是，方法名字参数可以随意定制，不像 EventBus 固定对象传输， 调用又要反射 `method.invoke()` ，但是这样就比较消耗内存了， 用空间换时间，相对传统方法快一点。调用相对蛋疼，容易写错。 如果大家想了解，**关注公众号**，跟我交流。

### 相比传统实现有哪些优势
1. 代码非常简洁。 在`Activity`或`Fragment`完全不需要手动调用`register`和`unregister`这样的代码。
2. 高效。 由于 javassist 是在 class 转 dex 的编译期实现的，动态代码插入，完成不涉及反射代码，比 EventBus 优秀。
3. 不关心线程切换问题。 只需要在注解中传入参数， 实现线程自定切换。

### 用法
事件接收用法跟 EventBus 类似， 注册跟反注册通过注解标记来完成，activity 跟fragment 无需注册反注册。
``` java
public class EventActivity extends AppCompatActivity {

    private EventInjectTestClass mEventInjectTestClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        mEventInjectTestClass = new EventInjectTestClass();
        mEventInjectTestClass.create(this);
    }

    @EventReceiver(EventTag.ON_TEST_EVENT_ACTIVITY)
    public void methodEventActivityReceive(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @EventReceiver(EventTag.ON_TEST_EVENT_NORMAL_CLASS)
    public void methodEventNormalClassReceive(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    public void onClickActivityEvent(View view) {
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_ACTIVITY, "来自activity事件对象");
    }

    public void onClickNormalClassEvent(View view) {
        EventCenterV2.getInstance().onEvent(EventTag.ON_TEST_EVENT_NORMAL_CLASS, "来自normalclass事件对象");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mEventInjectTestClass.destroy();
    }
}
```

``` java
public class EventInjectTestClass {

    private Context mContext;

    @EventRegister
    public void create(Context context) {

        mContext = context;
    }

    @EventUnRegister
    public void destroy() {

    }

    @EventReceiver(EventTag.ON_TEST_EVENT_ACTIVITY, thread = EventReceiver.MAIN)
    public void methodEventReceiveClass(String str) {//事件接收方法
        Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
    }

}
```

### 实现

注册监听实现

``` java
 public void register(int eventTag, IEventCaller caller, int thread) {
        if (caller == null) {
            Log.w(TAG, StringUtils.format("regist null caller, tag = %d", eventTag));
            return;
        }
        SparseArray<IEventCaller> callerList = new SparseArray<>();
        callerList.put(thread, caller);
        CopyOnWriteArrayList<SparseArray<IEventCaller>> exist = eventMap.get(eventTag);
        if (exist != null) {
            exist.add(callerList);
        } else {
            CopyOnWriteArrayList<SparseArray<IEventCaller>> handlerList = new CopyOnWriteArrayList<>();
            handlerList.add(callerList);
            eventMap.put(eventTag, handlerList);
        }
        searchTagMap.put(caller, eventTag);
        searchSparseMap.put(caller, callerList);
    }
```

反注册

``` java
  public void unRegister(IEventCaller caller) {
        if (caller == null) {
            Log.w(TAG, StringUtils.format("warning,  unRegister null caller"));
            return;
        }
        Integer eventTag = searchTagMap.get(caller);
        SparseArray<IEventCaller> handler = searchSparseMap.get(caller);
        if (eventTag == null || handler == null) {
            Log.w(TAG, StringUtils.format("fail unregist eventTag null or handler null, handler = %s, caller = %s",
                    handler == null ? "null" : handler.toString(), eventTag == null ? "null" : eventTag.toString()));
            return;
        }
        CopyOnWriteArrayList<SparseArray<IEventCaller>> exist = eventMap.get(eventTag);
        if (exist != null) {
            if (exist.remove(handler)) {
                searchTagMap.remove(caller);
                searchSparseMap.remove(caller);
                return;
            }
            Log.w(TAG, StringUtils.format("fail unregist non exist handler = %s, caller = %s", handler.toString(), caller.toString()));
            return;
        }
        Log.w(TAG, StringUtils.format("fail unregist non exist eventTag = %d", eventTag));
 }
```

事件分发

``` java
 public void onEvent(int tag, Object data) {
        CopyOnWriteArrayList<SparseArray<IEventCaller>> handlerList = eventMap.get(tag);
        if (handlerList == null) {
            return;
        }
        Message m = mHandler.obtainMessage();
        m.obj = data;
        m.what = tag;
        Log.d(TAG, StringUtils.format("on event tag = &d, size = %d", tag, handlerList.size()));
        for (SparseArray<IEventCaller> caller : handlerList) {
            handlerCall(m, caller.keyAt(0), caller.valueAt(0));
        }
  }
```

首先需要 plugin module 引入 dependencies ` compile 'org.javassist:javassist:3.20.0-GA'`来支持 javassist

以下是插件 javassist **核心代码**， 用来实现自动注入代码。原理就是插入代码，说白了就是字符串拼接。groovy的语法也非常简单，**完全兼容 Java**
 有一点我想吐槽，他的语法跟 kotlin 相似，但是有没有模仿到位，就是 groovy 不支持传入被调用方法少于他需要的参数，kotlin 可以。我觉得可以优化下。

``` groovy
ClassPool pool = ClassPool.getDefault();
pool.insertClassPath(filePath) //引入当前类路径
//引入 android jar包 否则找不到相关类
pool.insertClassPath(project.android.bootClasspath[0].toString())
CtClass cc = pool.get("EventCenterV2");
cc.getDeclaredMethods() //获取 class 的所有方法
ctMethod.getAnnotations //获取 方法的所有注解
ctMethod.insertAfter(injectSentence) //方法内部插入代码

CtMethod mInitEventMethod = CtNewMethod.make(methodStr, cc )
info.getCtClz().addMethod(mInitEventMethod)  // class 插入方法

cc.writeFile();//将内存的代码输出，调用了这个函数才真正修改了class
```

具体实现代码，大家可以去我的github查看 **[https://github.com/luomoBM/AopTest](https://github.com/luomoBM/AopTest)**
这个单独抽取出来当个 **事件分发库** 使用
> 目前事件通知接收方法只支持少于等于一个参数，如果需要支持多个参数可自行修改。

Javassist 还有很多 api 用法， 项目中没用到，如果大家感兴趣的话可以去 **[https://jboss-javassist.github.io/javassist/tutorial/tutorial.html](https://jboss-javassist.github.io/javassist/tutorial/tutorial.html)** 看看里面的文档，挺详细的，可以用 Google 翻译配合查看，别告诉我你不会科学上网。。

这里还有个 Gradle 脚本的 Api 文档 对于 Gradle 插件编写有帮助。**[http://google.github.io/android-gradle-dsl/javadoc/current/](http://google.github.io/android-gradle-dsl/javadoc/current/)**

**希望这个文章对大家有帮助，并且记录自己的成长，共勉。**

如果文章对你有帮助，大家可以赞赏支持鼓励下作者。
 有什么疑问可以关注下方我的公众号， 留言就行。
 ![我的个人公众号](http://p9iqqot9p.bkt.clouddn.com/wechatoa.jpg)