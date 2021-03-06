package com.hebaiyi.www.katakuri.imageLoader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class BitmapCompress {

    /**
     * Bitmap对象采样压缩
     */
    public static Bitmap sampleCompression(String path, int requestWidth, int requestHeight) {
        Log.d("sampleCompression: ",requestHeight+"");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        // 预加载
        BitmapFactory.decodeFile(path,options);
        // 计算采样值
        options.inSampleSize = calculateInSample(options, requestWidth, requestHeight);
        options.inJustDecodeBounds = false;
        // 真实加载
        return BitmapFactory.decodeFile(path,options);
    }

    /**
     * 采样值的计算
     *
     * @param options       bitmap配置参数
     * @param requestWidth  实际宽度
     * @param requestHeight 实际高度
     * @return 采样值
     */
    private static int calculateInSample(BitmapFactory.Options options,
                                         int requestWidth, int requestHeight) {
        final int optionWidth = options.outWidth;
        final int optionHeight = options.outHeight;
        int simpleSize = 1;
        if (optionHeight > requestHeight || optionWidth > requestWidth) {
            final int halfHeight = optionHeight / 2;
            final int halfWidth = optionWidth / 2;
            while ((halfHeight / simpleSize) >= requestHeight &&
                    (halfWidth / simpleSize) >= requestWidth) {
                simpleSize *= 2;
            }
        }
        return simpleSize;
    }


}
