# 盲人键盘







## 键盘布局设计

### 参数

- ```Java
  keyboardHeight=650;// 键盘长度
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

#### 平移策略

键盘可平移区域一定，优先平移，若已平移到边界，则对按键进行缩放，缩放策略为**线性缩放**，列缩放考虑最大缩放按键数目，行缩放不考虑，若按键压缩后的长宽小于按键最小长宽，则本次平移无效。指定按键不进行任何缩放。





