package com.boliao.sunshine.utils;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

/**
 *���ļ������ļ��д��ѹ����zip��ʽ
 * @author ysc
 */
public class ZipUtil {
    private static final Logger log = Logger.getLogger(ZipUtil.class);
        
    private ZipUtil(){};
    
    /**
     * APDPlat�е���Ҫ�������
     * ��jar�ļ��е�ĳ���ļ�����������ݸ��Ƶ�ĳ���ļ���
     * @param jar ������̬��Դ��jar��
     * @param subDir jar�а��������ƾ�̬��Դ���ļ�������
     * @param loc ��̬��Դ���Ƶ���Ŀ���ļ���
     * @param force Ŀ�꾲̬��Դ���ڵ�ʱ���Ƿ�ǿ�Ƹ���
     */
    public static void unZip(String jar, String subDir, String loc, boolean force){
        try {
            File base=new File(loc);
            if(!base.exists()){
                base.mkdirs();
            }
            
            ZipFile zip=new ZipFile(new File(jar));
            Enumeration<? extends ZipEntry> entrys = zip.entries();
            while(entrys.hasMoreElements()){
                ZipEntry entry = entrys.nextElement();
                String name=entry.getName();
                if(!name.startsWith(subDir)){
                    continue;
                }
                //ȥ��subDir
                name=name.replace(subDir,"").trim();
                if(name.length()<2){
                    log.debug(name+" ���� < 2");
                    continue;
                }
                if(entry.isDirectory()){
                    File dir=new File(base,name);
                    if(!dir.exists()){
                        dir.mkdirs();
                        log.debug("����Ŀ¼");
                    }else{
                        log.debug("Ŀ¼�Ѿ�����");
                    }
                    log.debug(name+" ��Ŀ¼");
                }else{
                    File file=new File(base,name);
                    if(file.exists() && force){
                        file.delete();
                    }
                    if(!file.exists()){
                        InputStream in=zip.getInputStream(entry);
//                        FileUtils.copyFile(in,file);
                        log.debug("�����ļ�");
                    }else{
                    }
                    log.debug(name+" ����Ŀ¼");
                }
            }
        } catch (ZipException ex) {
            log.error("�ļ���ѹʧ��",ex);
        } catch (IOException ex) {
            log.error("�ļ�����ʧ��",ex);
        }
    }
    
   /**
     * ����ZIP�ļ�
     * @param sourcePath �ļ����ļ���·��
     * @param zipPath ���ɵ�zip�ļ�����·���������ļ�����
     */
    public static void createZip(String sourcePath, String zipPath) {
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(zipPath);
            zos = new ZipOutputStream(fos);
            writeZip(new File(sourcePath), "", zos);
        } catch (FileNotFoundException e) {
            log.error("����ZIP�ļ�ʧ��",e);
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
            } catch (IOException e) {
                log.error("����ZIP�ļ�ʧ��",e);
            }

        }
    }
    
    private static void writeZip(File file, String parentPath, ZipOutputStream zos) {
        if(file.exists()){
            if(file.isDirectory()){//�����ļ���
                parentPath+=file.getName()+File.separator;
                File [] files=file.listFiles();
                for(File f:files){
                    writeZip(f, parentPath, zos);
                }
            }else{
                FileInputStream fis=null;
                try {
                    fis=new FileInputStream(file);
                    ZipEntry ze = new ZipEntry(parentPath + file.getName());
                    zos.putNextEntry(ze);
                    byte [] content=new byte[1024];
                    int len;
                    while((len=fis.read(content))!=-1){
                        zos.write(content,0,len);
                        zos.flush();
                    }
                    
                
                } catch (FileNotFoundException e) {
                    log.error("����ZIP�ļ�ʧ��",e);
                } catch (IOException e) {
                    log.error("����ZIP�ļ�ʧ��",e);
                }finally{
                    try {
                        if(fis!=null){
                            fis.close();
                        }
                    }catch(IOException e){
                        log.error("����ZIP�ļ�ʧ��",e);
                    }
                }
            }
        }
    }    
    public static void main(String[] args) {
        ZipUtil.createZip("D:/workspaceBoliao/spider/images/ʹ��CSS3�е�α����Ⱦ���", "D:/workspaceBoliao/spider/images/ʹ��CSS3�е�α����Ⱦ���.zip");
//        ZipUtil.createZip("D:\\workspaces\\netbeans\\APDPlat\\APDPlat_Web\\target\\APDPlat_Web-2.2\\platform\\index.jsp", "D:\\workspaces\\netbeans\\APDPlat\\APDPlat_Web\\target\\APDPlat_Web-2.2\\platform\\index.zip");
        
    }
}