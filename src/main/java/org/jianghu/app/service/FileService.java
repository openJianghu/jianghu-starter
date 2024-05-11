package org.jianghu.app.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import org.jianghu.app.common.BizEnum;
import org.jianghu.app.common.BizException;
import org.jianghu.app.common.JSONPathObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service("fileServiceSystem")
public class FileService {

    private static final String UPLOADED_FOLDER = System.getProperty("user.dir") + "/upload/";
    private static final String MULTIPART_TMP_PATH = System.getProperty("user.dir") + "/multipartTmp/";
    private static final String testUserId = "__userId__";
    private static final String downloadBasePath = "/" + System.getProperty("appId") + "/upload/";


    public void uploadFileChunkByStream(JSONPathObject actionData, MultipartFile[] files) throws Exception {
        if (files == null || files.length == 0) { throw new BizException(BizEnum.request_data_invalid); }
        String hash = actionData.eval("hash");
        String indexString = actionData.eval("indexString");
        String chunksPath = MULTIPART_TMP_PATH + testUserId + "/" + hash;
        String filepath = chunksPath + "/" + hash + "_" + indexString;
        if (!FileUtil.exist(chunksPath)) {
            FileUtil.mkdir(chunksPath);
        }
        MultipartFile file = files[0];
        FileUtil.writeBytes(file.getBytes(), filepath);
    }

    public Map<String, Object> uploadFileDone(JSONPathObject actionData) throws Exception {
        // 解析actionData
        String hash = actionData.eval("hash");
        String filename = actionData.eval("filename");
        Integer total = actionData.eval("total");
        Integer chunkSize = actionData.eval("chunkSize");
        String fileDirectory = actionData.eval("fileDirectory");
        String fileDesc = actionData.eval("fileDesc");
        String filenameStorage = actionData.eval("filenameStorage");
        if (filenameStorage == null) {
            filenameStorage = String.format("%d_%d_%s", DateUtil.date().getTime(), new Random().nextInt(999999), filename);
        }

        // 文件存储路径
        Path fileUploadPath = Paths.get(UPLOADED_FOLDER, fileDirectory);
        Files.createDirectories(fileUploadPath);
        String fileId = String.format("%s_%06d", DateUtil.date().getTime(), new Random().nextInt(999999));;
        String filePath = fileUploadPath.resolve(filenameStorage).toString();
        Path filePathObj = Paths.get(filePath);

        // 检查目录是否存在
        boolean isFileExists = Files.exists(fileUploadPath);
        if (!isFileExists) {
            Files.createDirectories(fileUploadPath);
        }

        // 读取所有分片文件
        String chunksPath = Paths.get(MULTIPART_TMP_PATH, testUserId, hash).toString();
        List<String> chunkPathList = Files.list(Paths.get(chunksPath))
                .map(path -> path.toAbsolutePath().toString())
                .sorted()
                .collect(Collectors.toList());

        // 检查分片数量是否正确
        if (chunkPathList.size() != total || chunkPathList.isEmpty()) {
            FileUtil.del(chunksPath);
            throw new BizException(BizEnum.file_is_incomplete);
        }

        // 合并分片文件
        if (chunkPathList.size() == 1) {
            FileUtil.copyFile(chunkPathList.get(0), filePath);
        }
        if (chunkPathList.size() > 1) {
            mergeFileList(chunkPathList, filePath);
        }
         FileUtil.del(chunksPath);

        // 检查MD5一致性
        String fileHash = calculateFileHash(filePathObj);
        if (!hash.equals(fileHash)) {
            throw new BizException(BizEnum.file_damaged);
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("fileId", fileId);
        resultData.put("fileDirectory", fileDirectory);
        resultData.put("filename", filename);
        resultData.put("fileDesc", fileDesc);
        resultData.put("filenameStorage", filenameStorage);
        resultData.put("downloadPath", fileDirectory + "/" + filenameStorage);
        resultData.put("fileSize", Files.size(filePathObj));
        resultData.put("fileType", Files.probeContentType(filePathObj));
        resultData.put("downloadBasePath", downloadBasePath);
        resultData.put("downloadTip", "https://xxx.xxx.xxx" + downloadBasePath + filenameStorage);
        return resultData;
    }

    private static void mergeFileList(List<String> sourceFilePathList, String targetFilePath) throws Exception {
        OutputStream outputStream = new FileOutputStream(targetFilePath);
        for (String sourceFile : sourceFilePathList) {
            try (InputStream inputStream = new FileInputStream(sourceFile)) {
                IoUtil.copy(inputStream, outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String calculateFileHash(Path path) throws Exception {
        byte[] fileBytes = Files.readAllBytes(path);
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(fileBytes);
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
