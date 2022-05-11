package foo.v5archstudygroup.exercises.backpressure.server;

import com.google.protobuf.ByteString;
import foo.v5archstudygroup.exercises.backpressure.messages.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.annotation.PreDestroy;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This is the class that simulates the data processing.
 */
@Service public class DataProcessor
{

	private static final Logger LOGGER = LoggerFactory.getLogger(DataProcessor.class);

	/**
	 * You are not allowed to increase the number of threads, but otherwise you can do whatever changes you want
	 * to the executor service.
	 */
	private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
	private final AtomicLong processed = new AtomicLong();

	@PreDestroy void destroy()
	{
		// Always remember to clean up your threads
		threadPool.shutdown();
	}

	public void enqueue(Messages.ProcessingRequest request)
			throws IOException
	{
		String uuid = request.getUuid();
		ByteString payload = request.getPayload();
		String tempDir = System.getProperty("java.io.tmpdir");
		File dir = new File(tempDir, "backpressure");
		if (!dir.exists())
		{
			dir.mkdir();
		}
		final File bytesFile = new File(dir, uuid);
		FileWriter writer = new FileWriter(bytesFile);
		writer.write(payload.toStringUtf8());
		threadPool.submit(() -> {
            try
            {
                InputStream is = new FileInputStream(bytesFile);
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                StreamUtils.copy(is, os);
                doProcess(Messages.ProcessingRequest.newBuilder().setUuid(UUID.randomUUID().toString())
                        .setPayload(ByteString.copyFrom(os.toByteArray())).build());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            } finally {
                bytesFile.delete();
            }
        });
	}

	/**
	 * You are not allowed to change this method!
	 */
	private void doProcess(Messages.ProcessingRequest request)
	{
		try
		{
			// Pretend we are doing something really heavy with this request. The more bytes we have, the longer it takes
			// to complete
			Thread.sleep(request.getPayload().size() / 8192);
			LOGGER.info("Done processing request {}. Processed: {}", request.getUuid(), processed.incrementAndGet());
		}
		catch (InterruptedException ex)
		{
			throw new RuntimeException(ex);
		}
	}
}
