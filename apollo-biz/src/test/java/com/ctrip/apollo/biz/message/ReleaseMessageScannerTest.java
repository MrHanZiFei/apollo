package com.ctrip.apollo.biz.message;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.SettableFuture;

import com.ctrip.apollo.biz.entity.ReleaseMessage;
import com.ctrip.apollo.biz.repository.ReleaseMessageRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ReleaseMessageScannerTest {
  private ReleaseMessageScanner releaseMessageScanner;
  @Mock
  private ReleaseMessageRepository releaseMessageRepository;
  @Mock
  private Environment env;
  private int databaseScanInterval;

  @Before
  public void setUp() throws Exception {
    releaseMessageScanner = new ReleaseMessageScanner();
    ReflectionTestUtils
        .setField(releaseMessageScanner, "releaseMessageRepository", releaseMessageRepository);
    ReflectionTestUtils.setField(releaseMessageScanner, "env", env);
    databaseScanInterval = 100; //100 ms
    when(env.getProperty("apollo.message-scan.interval")).thenReturn(String.valueOf(databaseScanInterval));
    releaseMessageScanner.afterPropertiesSet();
  }

  @Test
  public void testScanMessageAndNotifyMessageListener() throws Exception {
    SettableFuture<String> someListenerFuture = SettableFuture.create();
    MessageListener someListener = (message, channel) -> someListenerFuture.set(message);
    releaseMessageScanner.addMessageListener(someListener);

    String someMessage = "someMessage";
    long someId = 100;
    ReleaseMessage someReleaseMessage = assembleReleaseMessage(someId, someMessage);

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(0L)).thenReturn(
        Lists.newArrayList(someReleaseMessage));

    String someListenerMessage =
        someListenerFuture.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(someMessage, someListenerMessage);

    SettableFuture<String> anotherListenerFuture = SettableFuture.create();
    MessageListener anotherListener = (message, channel) -> anotherListenerFuture.set(message);
    releaseMessageScanner.addMessageListener(anotherListener);

    String anotherMessage = "anotherMessage";
    long anotherId = someId + 1;
    ReleaseMessage anotherReleaseMessage = assembleReleaseMessage(anotherId, anotherMessage);

    when(releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(someId)).thenReturn(
        Lists.newArrayList(anotherReleaseMessage));

    String anotherListenerMessage =
        anotherListenerFuture.get(5000, TimeUnit.MILLISECONDS);

    assertEquals(anotherMessage, anotherListenerMessage);

  }

  private ReleaseMessage assembleReleaseMessage(long id, String message) {
    ReleaseMessage releaseMessage = new ReleaseMessage();
    releaseMessage.setId(id);
    releaseMessage.setMessage(message);
    return releaseMessage;
  }
}
