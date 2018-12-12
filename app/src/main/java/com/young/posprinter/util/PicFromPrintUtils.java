package com.young.posprinter.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

/**
 * Created by Zhipe on 2018/1/22.
 */

public class PicFromPrintUtils {


    public void init() {
//        Gray = 0.29900 * R + 0.58700 * G + 0.11400 * B
    }

    /*************************************************************************
     * 我们的热敏打印机是RP-POS80S或RP-POS80P或RP-POS80CS或RP-POS80CP打印机
     * 360*360的图片，8个字节（8个像素点）是一个二进制，将二进制转化为十进制数值
     * y轴：24个像素点为一组，即360就是15组（0-14）
     * x轴：360个像素点（0-359）
     * 里面的每一组（24*360），每8个像素点为一个二进制，（每组有3个，3*8=24）
     **************************************************************************/
    /**
     * 把一张Bitmap图片转化为打印机可以打印的bit(将图片压缩为360*360)
     * 效率很高（相对于下面）
     *
     * @param bit
     * @return
     */
    public static byte[] draw2PxPoint(Bitmap bit) {
        byte[] data = new byte[16290];
        int k = 0;
        for (int j = 0; j < 15; j++) {
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33; // m=33时，选择24点双密度打印，分辨率达到200DPI。
            data[k++] = 0x68;
            data[k++] = 0x01;
            for (int i = 0; i < 360; i++) {
                for (int m = 0; m < 3; m++) {
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(i, j * 24 + m * 8 + n, bit);
                        data[k] += data[k] + b;
                    }
                    k++;
                }
            }
            data[k++] = 10;
        }
        return data;
    }

    public static byte[] uploadLogo(Bitmap bit) {
        int width = bit.getWidth();
        int height = bit.getHeight();
        int l = height % 8 == 0 ? height / 8 : height / 8 + 1;
        int i3 = width / 8 > 0 ? (width % 8 == 0 ? width / 8 : width / 8 + 1) : 1;
        byte[] data = new byte[i3 * 8 * l + 7];
        int k = 0;
        data[k++] = 0x1C;
        data[k++] = 0x71;
        data[k++] = 0x01;
        data[k++] = (byte) (i3 % 256);
        data[k++] = (byte) (i3 / 256);
        data[k++] = (byte) (l % 256);
        data[k++] = (byte) (l / 256);
        for (int i = 0; i < width; i++) {
            for (int i1 = 0; i1 < l; i1++) {
                int i2 = height - i1 * 8 >= 8 ? 8 : height - i1 * 8;
                byte b = 0;
                for (int j = 0; j < i2; j++) {
                    byte b1 = px2Byte(i, i1 * 8 + j, bit);
                    b = (byte) ((b << 1) + b1);
                }
                data[k++] = b;
            }
        }
        return data;


//        int width = bit.getWidth();
//        int height = bit.getHeight();
//        int xl = 70;
//        int xh = 0;
//        int yl = 70;
//        int yh = 0;
//
//        byte[] data = new byte[(xl + xh * 256) * ((yl + yh * 256) * 8) + 7];
//        int k = 0;
//        data[k++] = 0x1C;
//        data[k++] = 0x71;
//        data[k++] = 0x01;
//        data[k++] = (byte) xl;
//        data[k++] = (byte) xh;
//        data[k++] = (byte) yl;
//        data[k++] = (byte) yh;
//        for (int i = 0; i < width; i++) {
//            int l = height % 8 == 0 ? height / 8 : height / 8 + 1;
//            for (int i1 = 0; i1 < l; i1++) {
//                int i2 = height - i1 * 8 >= 8 ? 8 : height - i1 * 8;
//                byte b = 0;
//                for (int j = 0; j < i2; j++) {
//                    byte b1 = px2Byte(i, i1 * 8 + j, bit);
//                    b = (byte) (b << 1 + b1);
//                }
//                data[k++] = b;
//            }
//        }
//        return data;
    }

    public static byte[] disposeRaster(Bitmap bitmap) {

//        1d 76 30 00 03 00 09 00
//        FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF FF
//        FF FF FF


        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int temp = width % 8 == 0 ? width / 8 : width / 8 + 1;
        int Wh = temp / 256 > 255 ? 255 : temp / 256;
        int Wl = temp % 256;
        int Hh = height / 256 > 255 ? 255 : height / 256;
        int Hl = height % 256;
        int W = Wh * 256 + Wl;
        int H = Hh * 256 + Hl;

        Bitmap bit = Bitmap.createBitmap(W * 8, H, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bit);
        Paint paint = new Paint();
        canvas.drawColor(0xFFFFFFFF);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        int len = (W * H) + 10;
        byte[] data = new byte[len];
        int index = 0;
        //初始化打印机 清楚缓存
        data[index++] = 0x1B;
        data[index++] = 0x40;

        //打印光栅图
        data[index++] = 0x1D;
        data[index++] = 0x76;
        data[index++] = 0x30;
        data[index++] = 0x00;
        data[index++] = (byte) Wl;
        data[index++] = (byte) Wh;
        data[index++] = (byte) Hl;
        data[index++] = (byte) Hh;
        for (int i = 0; i < H; i++) {
            for (int j = 0; j < W; j++) {
                byte b = 0;
                for (int k = 0; k < 8; k++) {
                    byte b1 = px2Byte(j * 8 + k, i, bit);
                    b = (byte) ((b << 1) + b1);
                }
                data[index++] = b;
            }
        }
        return data;
    }

    public static byte[] draw3PxPoint(Bitmap bit) {
        int width = bit.getWidth();
        int height = bit.getHeight();
        int line = height / 24;
        if (height % 24 != 0) {
            line++;
        }
        if (width > 650) {
            width = 650;
        }
        int i1 = width / 256;
        int i2 = width % 256;
        Bitmap bitmap = Bitmap.createBitmap(width, line * 24, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        canvas.drawColor(0xFFFFFFFF);
        canvas.drawBitmap(bit, 0, (line * 24 - height) / 2, paint);
        byte[] data = new byte[width * 3 * line + line * 5];
        int k = 0;
        for (int j = 0; j < line; j++) {
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33; // m=33时，选择24点双密度打印，分辨率达到200DPI。
            data[k++] = (byte) i2;
            data[k++] = (byte) i1;
            for (int i = 0; i < width; i++) {
                for (int m = 0; m < 3; m++) {
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(i, j * 24 + m * 8 + n, bitmap);
                        data[k] += data[k] + b;
                    }
                    k++;
                }
            }
            data[k++] = 10;
        }
        return data;
    }

    public static byte[] draw4PxPoint(Bitmap bit) {
        int width = bit.getWidth();
        int height = bit.getHeight();
        int line = height / 24;
        if (height % 24 != 0) {
            line++;
        }
        if (width > 650) {
            width = 650;
        }
        int i1 = width / 256;
        int i2 = width % 256;
        Bitmap bitmap = Bitmap.createBitmap(width, line * 24, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        canvas.drawColor(0xFFFFFFFF);
        canvas.drawBitmap(bit, 0, (line * 24 - height) / 2, paint);

        int len = width * 3 * line + line * 6 + 5;
        byte[] data = new byte[len];
        int k = 0;
        data[k++] = 0x1B;
        data[k++] = 0x33;
        data[k++] = 0x00;
        for (int j = 0; j < line; j++) {
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33; // m=33时，选择24点双密度打印，分辨率达到200DPI。
            data[k++] = (byte) i2;
            data[k++] = (byte) i1;
            for (int i = 0; i < width; i++) {
                for (int m = 0; m < 3; m++) {
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(i, j * 24 + m * 8 + n, bitmap);
                        data[k] += data[k] + b;
                    }
                    k++;
                }
            }
            if (k < data.length - 4) {
                data[k++] = 10;
            }
        }
        data[k++] = 0x1B;
        data[k++] = 0x33;
        data[k++] = 0x28;
        return data;
    }


    /**
     * 把一张Bitmap图片转化为打印机可以打印的bit
     *
     * @param bit
     * @return
     */
    public static byte[] pic2PxPoint(Bitmap bit) {
        long start = System.currentTimeMillis();
        byte[] data = new byte[16290];
        int k = 0;
        for (int i = 0; i < 15; i++) {
            data[k++] = 0x1B;
            data[k++] = 0x2A;
            data[k++] = 33; // m=33时，选择24点双密度打印，分辨率达到200DPI。
            data[k++] = 0x68;
            data[k++] = 0x01;
            for (int x = 0; x < 360; x++) {
                for (int m = 0; m < 3; m++) {
                    byte[] by = new byte[8];
                    for (int n = 0; n < 8; n++) {
                        byte b = px2Byte(x, i * 24 + m * 8 + 7 - n, bit);
                        by[n] = b;
                    }
                    data[k] = (byte) changePointPx1(by);
                    k++;
                }
            }
            data[k++] = 10;
        }
        long end = System.currentTimeMillis();
        long str = end - start;
        Log.i("TAG", "str:" + str);
        return data;
    }

    /**
     * 图片二值化，黑色是1，白色是0
     *
     * @param x   横坐标
     * @param y   纵坐标
     * @param bit 位图
     * @return
     */
    public static byte px2Byte(int x, int y, Bitmap bit) {
        byte b;
        int pixel = bit.getPixel(x, y);
        int red = (pixel & 0x00ff0000) >> 16; // 取高两位
        int green = (pixel & 0x0000ff00) >> 8; // 取中两位
        int blue = pixel & 0x000000ff; // 取低两位
        int gray = RGB2Gray(red, green, blue);
        if (gray < 128) {
            b = 1;
        } else {
            b = 0;
        }
        return b;
    }

    /**
     * 图片灰度的转化
     *
     * @param r
     * @param g
     * @param b
     * @return
     */
    private static int RGB2Gray(int r, int g, int b) {
        int gray = (int) (0.29900 * r + 0.58700 * g + 0.11400 * b);  //灰度转化公式
        return gray;
    }

    /**
     * 对图片进行压缩（去除透明度）
     *
     * @param bitmapOrg
     */
    public static Bitmap compressPic(Bitmap bitmapOrg) {
        // 获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        // 定义预转换成的图片的宽度和高度
        int newWidth = 360;
        int newHeight = 360;
        Bitmap targetBmp = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888);
        Canvas targetCanvas = new Canvas(targetBmp);
        targetCanvas.drawColor(0xffffffff);
        targetCanvas.drawBitmap(bitmapOrg, new Rect(0, 0, width, height), new Rect(0, 0, newWidth, newHeight), null);
        return targetBmp;
    }


    /**
     * 对图片进行压缩(不去除透明度)
     *
     * @param bitmapOrg
     */
    public static Bitmap compressBitmap(Bitmap bitmapOrg) {
        // 加载需要操作的图片，这里是一张图片
//        Bitmap bitmapOrg = BitmapFactory.decodeResource(getResources(),R.drawable.alipay);
        // 获取这个图片的宽和高
        int width = bitmapOrg.getWidth();
        int height = bitmapOrg.getHeight();
        // 定义预转换成的图片的宽度和高度
        int newWidth = 360;
        int newHeight = 360;
        // 计算缩放率，新尺寸除原始尺寸
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 创建操作图片用的matrix对象
        Matrix matrix = new Matrix();
        // 缩放图片动作
        matrix.postScale(scaleWidth, scaleHeight);
        // 创建新的图片
        Bitmap resizedBitmap = Bitmap.createBitmap(bitmapOrg, 0, 0, width, height, matrix, true);
        // 将上面创建的Bitmap转换成Drawable对象，使得其可以使用在ImageView, ImageButton中
//        BitmapDrawable bmd = new BitmapDrawable(resizedBitmap);
        return resizedBitmap;
    }

    /**
     * 将[1,0,0,1,0,0,0,1]这样的二进制转为化十进制的数值（效率更高）
     *
     * @param arry
     * @return
     */
    public static int changePointPx1(byte[] arry) {
        int v = 0;
        for (int j = 0; j < arry.length; j++) {
            if (arry[j] == 1) {
                v = v | 1 << j;
            }
        }
        return v;
    }

    /**
     * 将[1,0,0,1,0,0,0,1]这样的二进制转为化十进制的数值
     *
     * @param arry
     * @return
     */
    public byte changePointPx(byte[] arry) {
        byte v = 0;
        for (int i = 0; i < 8; i++) {
            v += v + arry[i];
        }
        return v;
    }

    /**
     * 得到位图的某个点的像素值
     *
     * @param bitmap
     * @return
     */
    public byte[] getPicPx(Bitmap bitmap) {
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];// 保存所有的像素的数组，图片宽×高
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < pixels.length; i++) {
            int clr = pixels[i];
            int red = (clr & 0x00ff0000) >> 16; // 取高两位
            int green = (clr & 0x0000ff00) >> 8; // 取中两位
            int blue = clr & 0x000000ff; // 取低两位
            System.out.println("r=" + red + ",g=" + green + ",b=" + blue);
        }
        return null;
    }
}
