package net.yrom.screenrecorder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.TextView
import com.obs.services.ObsClient
import com.obs.services.ObsConfiguration
import com.obs.services.exception.ObsException
import com.obs.services.model.AuthTypeEnum
import com.obs.services.model.PostSignatureRequest
import com.tbruyelle.rxpermissions2.RxPermissions
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

/**
 * @author Dsh  imkobedroid@gmail.com
 * @date 2021/1/8
 */
class PostObjectSample : Activity() {
    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.test)
        sb = StringBuffer()
        val config = ObsConfiguration()
        config.endPoint = endPoint
        config.authType = authType
        obsClient = ObsClient(ak, sk, config)
        val tv = findViewById<TextView>(R.id.tv)
        tv.text = "Click to start test"
        tv.setOnClickListener { v: View? ->
            val rxPermissions = RxPermissions(this)
            rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe { granted: Boolean ->
                        if (granted) {
                            val task: AsyncTask<Void, Void, String> = PostObjectTask()
                            task.execute()
                        }
                    }
        }
    }

    internal inner class PostObjectTask : AsyncTask<Void?, Void?, String>() {
        protected override fun doInBackground(vararg params: Void): String {
            return try {

                /*
                 * Create bucket
                 */
                sb!!.append("Create a new bucket for demo\n\n")
                obsClient!!.createBucket(bucketName)

                /*
                 * Create sample file
                 */
                val sampleFile = createSampleFile()

                /*
                 * Claim a post object request
                 */
                val request = PostSignatureRequest()
                request.expires = 3600
                val formParams: MutableMap<String, Any> = HashMap()
                val contentType = "text/plain"
                if (authType == AuthTypeEnum.OBS) {
                    formParams["x-obs-acl"] = "public-read"
                } else {
                    formParams["acl"] = "public-read"
                }
                formParams["content-type"] = contentType
                request.formParams = formParams
                val response = obsClient!!.createPostSignature(request)
                formParams["key"] = objectKey
                formParams["policy"] = response.policy
                if (authType == AuthTypeEnum.OBS) {
                    formParams["signature"] = response.signature
                    formParams["accesskeyid"] = ak
                } else {
                    formParams["signature"] = response.signature
                    formParams["AwsAccesskeyid"] = ak
                }
                val postUrl = "$bucketName.$endPoint"
                sb!!.append("Creating object in browser-based way")
                sb!!.append("\tpost url:$postUrl")
                val res = formUpload(postUrl, formParams, sampleFile, contentType)
                sb!!.append("\tresponse:$res")
                sb.toString()
            } catch (e: ObsException) {
                sb!!.append("\n\n")
                sb!!.append("Response Code:" + e.responseCode)
                        .append("\n\n")
                        .append("Error Message:" + e.errorMessage)
                        .append("\n\n")
                        .append("Error Code:" + e.errorCode)
                        .append("\n\n")
                        .append("Request ID:" + e.errorRequestId)
                        .append("\n\n")
                        .append("Host ID:" + e.errorHostId)
                sb.toString()
            } catch (e: Exception) {
                sb!!.append("\n\n")
                sb!!.append(e.message)
                sb.toString()
            } finally {
                if (obsClient != null) {
                    try {
                        obsClient!!.close()
                    } catch (e: IOException) {
                    }
                }
            }
        }

        override fun onPostExecute(result: String) {
            val tv = findViewById<View>(R.id.tv) as TextView
            tv.text = result
            tv.movementMethod = ScrollingMovementMethod.getInstance()
        }

        private fun formUpload(postUrl: String, formFields: Map<String, Any>?, sampleFile: File, contentType: String): String {
            var contentType: String? = contentType
            var res = ""
            var conn: HttpURLConnection? = null
            val boundary = "9431149156168"
            var reader: BufferedReader? = null
            var `in`: DataInputStream? = null
            var out: OutputStream? = null
            try {
                val url = URL(postUrl)
                conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn!!.readTimeout = 30000
                conn.doOutput = true
                conn.doInput = true
                conn.requestMethod = "POST"
                conn.setRequestProperty("User-Agent", "OBS/Test")
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                out = DataOutputStream(conn.outputStream)

                // text
                if (formFields != null) {
                    val strBuf = StringBuffer()
                    val iter = formFields.entries.iterator()
                    var i = 0
                    while (iter.hasNext()) {
                        val entry = iter.next()
                        val inputName = entry.key
                        val inputValue = entry.value ?: continue
                        if (i == 0) {
                            strBuf.append("--").append(boundary).append("\r\n")
                            strBuf.append("Content-Disposition: form-data; name=\"$inputName\"\r\n\r\n")
                            strBuf.append(inputValue)
                        } else {
                            strBuf.append("\r\n").append("--").append(boundary).append("\r\n")
                            strBuf.append("Content-Disposition: form-data; name=\"$inputName\"\r\n\r\n")
                            strBuf.append(inputValue)
                        }
                        i++
                    }
                    out.write(strBuf.toString().toByteArray())
                }

                // file
                val filename = sampleFile.name
                if (contentType == null || contentType == "") {
                    contentType = "application/octet-stream"
                }
                var strBuf = StringBuffer()
                strBuf.append("\r\n").append("--").append(boundary).append("\r\n")
                strBuf.append("Content-Disposition: form-data; name=\"file\"; filename=\"$filename\"\r\n")
                strBuf.append("Content-Type: $contentType\r\n\r\n")
                out.write(strBuf.toString().toByteArray())
                `in` = DataInputStream(FileInputStream(sampleFile))
                var bytes = 0
                val bufferOut = ByteArray(1024)
                while (`in`.read(bufferOut).also { bytes = it } != -1) {
                    out.write(bufferOut, 0, bytes)
                }
                val endData = "\r\n--$boundary--\r\n".toByteArray()
                out.write(endData)
                out.flush()

                // 读取返回数据
                strBuf = StringBuffer()
                reader = BufferedReader(InputStreamReader(conn.inputStream))
                var line: String? = null
                while (reader.readLine().also { line = it } != null) {
                    strBuf.append(line).append("\n")
                }
                res = strBuf.toString()
            } catch (e: Exception) {
                sb!!.append("\n\n")
                sb!!.append("Send post request exception: $e")
                e.printStackTrace()
            } finally {
                if (out != null) {
                    try {
                        out.close()
                    } catch (e: IOException) {
                    }
                }
                if (`in` != null) {
                    try {
                        `in`.close()
                    } catch (e: IOException) {
                    }
                }
                if (reader != null) {
                    try {
                        reader.close()
                    } catch (e: IOException) {
                    }
                }
                if (conn != null) {
                    conn.disconnect()
                    conn = null
                }
            }
            return res
        }

        @Throws(IOException::class)
        private fun createSampleFile(): File {
            val file = File.createTempFile("obs-android-sdk-", ".txt")
            file.deleteOnExit()
            val writer: Writer = OutputStreamWriter(FileOutputStream(file))
            writer.write("abcdefghijklmnopqrstuvwxyz\n")
            writer.write("0123456789011234567890\n")
            writer.close()
            return file
        }
    }

    companion object {
        private const val endPoint = "your-endpoint"
        private const val ak = "ZR81WJKEIS1SCBCGH8BY"
        private const val sk = "9nwWBDGuNUgQiYHMqEYWWIHJn5SjdSx8JBIKmtGC"
        private const val bucketName = "my-obs-bucket-demo"
        private const val objectKey = "my-obs-object-key-demo"
        private var obsClient: ObsClient? = null
        private var sb: StringBuffer? = null
        private val authType = AuthTypeEnum.OBS
    }
}