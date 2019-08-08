/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.api.workspace.activity;

import static java.util.Collections.singleton;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import com.google.common.collect.ImmutableMap;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.WorkspaceRuntimes;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.shared.Constants;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(value = MockitoTestNGListener.class)
public class WorkspaceActivityCheckerTest {
  private static final long DEFAULT_TIMEOUT = 60_000L; // 1 minute

  private ManualClock clock;
  private WorkspaceActivityChecker checker;
  @Mock private WorkspaceManager workspaceManager;
  @Mock private WorkspaceRuntimes workspaceRuntimes;
  @Mock private WorkspaceActivityDao workspaceActivityDao;
  @Mock private EventService eventService;

  @BeforeMethod
  public void setUp() {
    clock = new ManualClock();

    WorkspaceActivityManager activityManager =
        new WorkspaceActivityManager(
            workspaceManager, workspaceActivityDao, eventService, DEFAULT_TIMEOUT, clock);

    checker =
        new WorkspaceActivityChecker(
            workspaceActivityDao, workspaceManager, workspaceRuntimes, activityManager, clock);
  }

  @Test
  public void shouldStopAllExpiredWorkspaces() throws Exception {
    when(workspaceActivityDao.findExpired(anyLong())).thenReturn(Arrays.asList("1", "2", "3"));

    checker.validate();

    verify(workspaceActivityDao, times(3)).removeExpiration(anyString());
    verify(workspaceActivityDao).removeExpiration(eq("1"));
    verify(workspaceActivityDao).removeExpiration(eq("2"));
    verify(workspaceActivityDao).removeExpiration(eq("3"));
  }

  @Test
  public void shouldRecreateMissingActivityRecord() throws Exception {
    // given
    String id = "1";
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(null);
    when(workspaceManager.getWorkspace(eq(id)))
        .thenReturn(
            WorkspaceImpl.builder()
                .setId(id)
                .setAttributes(ImmutableMap.of(Constants.CREATED_ATTRIBUTE_NAME, "15"))
                .build());

    // when
    clock.forward(Duration.of(1, ChronoUnit.SECONDS));
    checker.validate();

    // then
    ArgumentCaptor<WorkspaceActivity> captor = ArgumentCaptor.forClass(WorkspaceActivity.class);
    verify(workspaceActivityDao).createActivity(captor.capture());
    WorkspaceActivity created = captor.getValue();
    assertEquals(id, created.getWorkspaceId());
    assertEquals(Long.valueOf(15), created.getCreated());
    assertEquals(WorkspaceStatus.RUNNING, created.getStatus());
    assertNotNull(created.getLastRunning());
    assertEquals(clock.millis(), (long) created.getLastRunning());
    assertNotNull(created.getExpiration());
    assertEquals(clock.millis() + DEFAULT_TIMEOUT, (long) created.getExpiration());
  }

  @Test
  public void shouldRestoreCreatedTimeOnInvalidActivityRecord() throws Exception {
    // given
    String id = "1";
    WorkspaceActivity invalidActivity = new WorkspaceActivity();
    invalidActivity.setWorkspaceId(id);
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(invalidActivity);
    when(workspaceManager.getWorkspace(eq(id)))
        .thenReturn(
            WorkspaceImpl.builder()
                .setId(id)
                .setAttributes(ImmutableMap.of(Constants.CREATED_ATTRIBUTE_NAME, "15"))
                .build());

    // when
    checker.validate();

    // then
    verify(workspaceActivityDao).setCreatedTime(eq(id), eq(15L));
  }

  @Test
  public void shouldRestoreLastRunningTimeOnInvalidActivityRecordUsingCreatedTime()
      throws Exception {
    // given
    String id = "1";
    WorkspaceActivity invalidActivity = new WorkspaceActivity();
    invalidActivity.setWorkspaceId(id);
    invalidActivity.setCreated(15);
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(invalidActivity);

    // when
    clock.forward(Duration.of(1, ChronoUnit.SECONDS));
    checker.validate();

    // then
    verify(workspaceActivityDao, never()).setCreatedTime(eq(id), anyLong());
    verify(workspaceActivityDao)
        .setStatusChangeTime(eq(id), eq(WorkspaceStatus.RUNNING), eq(clock.millis()));
  }

  @Test
  public void shouldRestoreLastRunningTimeOnInvalidActivityRecordUsingLastStartingTime()
      throws Exception {
    // given
    String id = "1";
    WorkspaceActivity invalidActivity = new WorkspaceActivity();
    invalidActivity.setWorkspaceId(id);
    invalidActivity.setLastStarting(10);
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(invalidActivity);
    when(workspaceManager.getWorkspace(eq(id)))
        .thenReturn(
            WorkspaceImpl.builder()
                .setId(id)
                .setAttributes(ImmutableMap.of(Constants.CREATED_ATTRIBUTE_NAME, "15"))
                .build());

    // when
    clock.forward(Duration.of(1, ChronoUnit.SECONDS));
    checker.validate();

    // then
    verify(workspaceActivityDao).setCreatedTime(eq(id), eq(15L));
    verify(workspaceActivityDao)
        .setStatusChangeTime(eq(id), eq(WorkspaceStatus.RUNNING), eq(clock.millis()));
  }

  @Test
  public void shouldRestoreExpirationTimeMoreThanASecondAfterRunning() throws Exception {
    long lastRunning = clock.millis();
    String id = "1";
    WorkspaceActivity invalidActivity = new WorkspaceActivity();
    invalidActivity.setWorkspaceId(id);
    invalidActivity.setCreated(clock.millis());
    invalidActivity.setLastRunning(lastRunning);
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(invalidActivity);

    // when
    clock.forward(Duration.of(1500, ChronoUnit.MILLIS));
    checker.validate();

    // then
    verify(workspaceActivityDao).setExpirationTime(eq(id), eq(lastRunning + DEFAULT_TIMEOUT));
  }

  @Test
  public void shouldNotRestoreExpirationTimeLessThanASecondAfterRunning() throws Exception {
    String id = "1";
    WorkspaceActivity invalidActivity = new WorkspaceActivity();
    invalidActivity.setWorkspaceId(id);
    invalidActivity.setCreated(clock.millis());
    invalidActivity.setLastRunning(clock.millis());
    when(workspaceRuntimes.getRunning()).thenReturn(singleton(id));
    when(workspaceActivityDao.findActivity(eq(id))).thenReturn(invalidActivity);

    // when
    clock.forward(Duration.of(900, ChronoUnit.MILLIS));
    checker.validate();

    // then
    verify(workspaceActivityDao, never()).setExpirationTime(anyString(), anyLong());
  }

  private static final class ManualClock extends Clock {

    private Instant instant;

    private ManualClock() {
      instant = Instant.now();
    }

    @Override
    public ZoneId getZone() {
      return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
      return instant;
    }

    public void forward(Duration duration) {
      instant = instant.plus(duration);
    }
  }
}
