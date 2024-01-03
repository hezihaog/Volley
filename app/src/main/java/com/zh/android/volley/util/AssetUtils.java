package com.zh.android.volley.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Assets工具类
 */
public class AssetUtils {
    /**
     * 将asserts目录中的所有文件和文件夹复制到应用的内部存储空间的某个目录（例如：/data/data/your.package.name/files）
     *
     * @param assetsPath 目标文件
     * @param savePath   拷贝到的目录
     */
    public static void copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        try {
            // 获取assets目录下的所有文件及目录名
            String[] fileNames = context.getAssets().list(assetsPath);
            // 如果是目录
            if (fileNames != null && fileNames.length > 0) {
                File file = new File(savePath);
                // 如果文件夹不存在，则递归
                file.mkdirs();
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {
                // 如果是文件
                InputStream is = context.getAssets().open(assetsPath);
                FileOutputStream fos = new FileOutputStream(new File(savePath));
                byte[] buffer = new byte[1024];
                int byteCount;
                // 循环从输入流读取
                while ((byteCount = is.read(buffer)) != -1) {
                    // 将读取的输入流写入到输出流
                    fos.write(buffer, 0, byteCount);
                }
                // 刷新缓冲区
                fos.flush();
                is.close();
                fos.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
