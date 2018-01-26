# 盲人键盘

## 键盘布局设计

### 参数

所有参数均可在右上角菜单处修改，以下显示的均是默认值

- ```Java
  keyboardHeight=500;// 键盘长度
  ```

- ```Java
  keyboardWidth=1438;// 键盘宽度
  ```

- ```java
  deltaY=100;// 键盘与键盘区域上边界距离
  ```

- ```java
  topThreshold=0;// 键盘上边界与键盘区域上边界最小距离
  ```

- ```java
  bottomThreshold=955;// 键盘下边界与键盘区域上边界最大距离
  ```

- ```java
  minWidth=72;// 键盘按键最小宽度
  ```

- ```java
  minHetight;// 键盘按键最小长度
  ```

- ```java
  scalingNum=3;// 最大缩放按键数目
  ```

- ```java
  try_layout_mode;// 键盘平移策略
  // RESPECTIVELY_MOVEMENT:行与行之间分别平移
  // BODILY_MOVEMENT:键盘整体平移
  ```

- ```java
  getKey_mode;// 获取按键策略
  // LOOSE_MODE：宽松策略，采取最近原则，超出键盘边界仍可以获取按键
  // STRICT_MODE：严格策略，必须按在按键区域内才算做正确获取
  ```

- ```java
  tap_range=0.8;// 平移指数，平移到指定按键中心附近一定距离d内（最小原则），按键宽度为D，则tap_range=2*d/D
  ```

- ```java
  scalingMode;// 缩放策略
  // LINEAR_MODE:线性缩放策略
  // EXPONENT_MODE:指数缩放策略,由exponent作为参数控制
  ```

- ```java
  exponent=0.1;// 指数缩放策略指数
  // 例：Num=3(需要缩放的按键个数为3),index(从靠中间数起，缩放的第index个按键,从0开始计数),Line=TOP(第一行缩放，每一行缩放的策略不同)
  a=exponent;
  ratio=exp(a*(length-index-1))/(1+exp(a)+exp(2*a));// 第index个按键宽度占三个按键总宽度比例

  ```

#### 平移策略

键盘可平移区域一定，指定按键不进行任何缩放。先进行平移，后进行列平移。

- 行平移：优先平移，若已平移到边界，则对按键进行缩放，不考虑最大缩放按键数目，采用线性缩放策略，若按键压缩后的长度小于按键最小长度，则本次行平移无效。
- 列平移，先对中间部分列平移，剩余列进行缩放，遵从缩放策略（线性或者指数），若按键压缩后的宽度小于按键最小宽度，则本次列平移无效。





