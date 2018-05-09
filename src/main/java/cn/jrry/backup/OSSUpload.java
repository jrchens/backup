package cn.jrry.backup;

import java.io.File;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.utils.CRC64;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.UploadFileRequest;
import com.aliyun.oss.model.UploadFileResult;
import com.google.common.io.Files;

public class OSSUpload {

	private static String accessKeyId = "$accessKeyId";
	private static String accessKeySecret = "$accessKeySecret";
	// private static String bucketName = "$bucketName";

	 private static String endpoint =
	 "http://oss-cn-shanghai-internal.aliyuncs.com";
//	private static String endpoint = "http://oss-cn-shanghai.aliyuncs.com";

	private static final Logger logger = LoggerFactory.getLogger(OSSUpload.class);

	public static void main(String[] args) {
		try {

			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1-7
			int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR); // 1-52

			String dir = args[0];
			for (int i = 1; i < args.length; i++) {
				String filename = args[i];
				String key = filename.substring(filename.lastIndexOf("/") + 1);
				String prefix = key.substring(0, key.length() - 7);
				if (dayOfWeek == 1) {
					String week = String.valueOf(weekOfYear);
					if (weekOfYear < 10) {
						week = "0".concat(week);
					}
					key = prefix.concat("-")
							.concat(String.valueOf(year).concat(week).concat(key.substring(key.length() - 7)));
				} else {
					key = prefix.concat("-").concat(String.valueOf(dayOfWeek).concat(key.substring(key.length() - 7)));
				}
				upload(filename, dir,key);
			}
		} catch (Exception e) {
			logger.error("main error", e);
		}
		System.exit(0);
	}

	public static void upload(String uploadFile, String bucketName, String key) {
		OSSClient ossClient = null;

		try {
			ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
			File file = new File(uploadFile);

			UploadFileRequest uploadFileRequest = null;

			boolean exists = ossClient.doesObjectExist(bucketName, key);
			if (exists) {
				ossClient.deleteObject(bucketName, key);
			}

			uploadFileRequest = new UploadFileRequest(bucketName, key);

			// 待上传的本地文件
			uploadFileRequest.setUploadFile(uploadFile);
			// 设置并发下载数，默认1
			uploadFileRequest.setTaskNum(5);
			// 设置分片大小，默认100KB
			uploadFileRequest.setPartSize(1024 * 1024 * 1);
			// 开启断点续传，默认关闭
			uploadFileRequest.setEnableCheckpoint(true);

			UploadFileResult uploadResult = ossClient.uploadFile(uploadFileRequest);

			CompleteMultipartUploadResult multipartUploadResult = uploadResult.getMultipartUploadResult();
			String etag = multipartUploadResult.getETag();

			byte[] bs = Files.toByteArray(file);
			CRC64 crc64 = new CRC64();
			crc64.update(bs, bs.length);

			long clientCRC64 = crc64.getValue();
			long serverCRC64 = multipartUploadResult.getServerCRC().longValue();
			if (clientCRC64 == serverCRC64) {
				logger.info("upload file ({}) etag:{},checksum success ({},{})", file.getName(), etag, clientCRC64,
						serverCRC64);
			} else {
				logger.error("upload file ({}) etag:{},checksum error ({},{})", file.getName(), etag, clientCRC64,
						serverCRC64);
			}

		} catch (OSSException oe) {
			// System.out.println("Caught an OSSException, which means your
			// request made it to OSS, "
			// + "but was rejected with an error response for some reason.");
			// System.out.println("Error Message: " + oe.getErrorCode());
			// System.out.println("Error Code: " + oe.getErrorCode());
			// System.out.println("Request ID: " + oe.getRequestId());
			// System.out.println("Host ID: " + oe.getHostId());
			logger.error(
					"Caught an OSSException, which means your request made it to OSS,but was rejected with an error response for some reason.",
					oe);
		} catch (ClientException ce) {
			// System.out.println("Caught an ClientException, which means the
			// client encountered "
			// + "a serious internal problem while trying to communicate with
			// OSS, "
			// + "such as not being able to access the network.");
			// System.out.println("Error Message: " + ce.getMessage());

			logger.error(
					"Caught an ClientException, which means the client encountered a serious internal problem while trying to communicate with OSS, such as not being able to access the network.",
					ce);
		} catch (Throwable e) {
			logger.error("Upload file to OSS failed", e);
		} finally {
			ossClient.shutdown();
		}
	}

}
