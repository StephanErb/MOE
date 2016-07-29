/*
 * Copyright (c) 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.devtools.moe.client.repositories;

import static com.google.common.truth.Truth.assertThat;

import junit.framework.TestCase;

import org.joda.time.DateTime;

import java.util.concurrent.atomic.AtomicBoolean;

/** Unit test for the {@link OriginalAuthorMetadataScrubber}. */
public class OriginalAuthorMetadataScrubberTest extends TestCase {
  private final OriginalAuthorMetadataScrubber oams = new OriginalAuthorMetadataScrubber();

  private final AtomicBoolean shouldScrub = new AtomicBoolean(false);
  private final MetadataScrubberConfig config =
      new MetadataScrubberConfig() {
        @Override
        public boolean getRestoreOriginalAuthor() {
          return shouldScrub.get();
        }
      };

  public void testOriginalAuthorSubstitution() throws Exception {
    shouldScrub.set(true);
    RevisionMetadata initial =
        RevisionMetadata.builder()
            .id("100")
            .author("foo@bar.com")
            .date(new DateTime(1L))
            .description(
                "random description\n"
                    + "ORIGINAL_AUTHOR=Blah Foo <blah@foo.com>\n"
                    + "blahblahblah")
            .build();

    RevisionMetadata expected =
        RevisionMetadata.builder()
            .id("100")
            .author("Blah Foo <blah@foo.com>")
            .date(new DateTime(1L))
            .description("random description\n\nblahblahblah")
            .build();

    RevisionMetadata actual = oams.scrub(initial, config);
    assertThat(actual.author()).isEqualTo("Blah Foo <blah@foo.com>");
    assertThat(actual.description()).isEqualTo(expected.description());
  }

  public void testOriginalAuthorSubstitution_Disabled() throws Exception {
    shouldScrub.set(false);
    RevisionMetadata initial =
        RevisionMetadata.builder()
            .id("100")
            .author("foo@bar.com")
            .date(new DateTime(1L))
            .description(
                "random description\n"
                    + "ORIGINAL_AUTHOR=\"Blah Foo <blah@foo.com>\"\n"
                    + "blahblahblah")
            .build();

    RevisionMetadata actual = oams.scrub(initial, config);
    assertThat(actual).isEqualTo(initial);
    assertThat(actual).isSameAs(initial);
  }

  public void testExtractField() {
    String desc = "blahfoo\nORIGINAL_AUTHOR=blah@foo=asdf\nblahblahblah";
    String after = oams.extractFirstOriginalAuthorField(desc);
    assertThat(after).isEqualTo("blahfoo\n\nblahblahblah");
  }
}