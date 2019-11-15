/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.snapshot

import org.gradle.internal.file.FileType
import spock.lang.Specification
import spock.lang.Unroll

import javax.annotation.Nullable
import java.util.stream.Collectors

@Unroll
abstract class AbstractIncompleteSnapshotWithChildrenTest<T extends FileSystemNode> extends Specification {

    FileSystemNode initialRoot
    List<FileSystemNode> children
    String absolutePath
    int offset
    FileSystemNode selectedChild

    abstract protected T createInitialRootNode(String pathToParent, List<FileSystemNode> children);

    abstract protected boolean isSameNodeType(FileSystemNode node)

    def "invalidate child with no common pathToParent has no effect (#vfsSpec)"() {
        setupTest(vfsSpec)

        expect:
        initialRoot.invalidate(absolutePath, offset).get() is initialRoot

        where:
        vfsSpec << NO_COMMON_PREFIX + COMMON_PREFIX
    }

    def "store #fileType child with common prefix adds a new child with the shared prefix of type Directory (#vfsSpec)"() {
        setupTest(vfsSpec)
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        AbstractIncompleteSnapshotWithChildren newChild = getNodeWithIndexOfSelectedChild(resultRoot.children)
        then:
        resultRoot.children == childrenWithSelectedChildReplacedBy(newChild)
        newChild.pathToParent == commonPrefix
        newChild.children == sortedChildren(snapshot, selectedChild)
        newChild.type == FileType.Directory
        1 * snapshot.withPathToParent(snapshot.pathToParent.substring(commonPrefix.length() + 1)) >> snapshot
        1 * selectedChild.withPathToParent(selectedChild.pathToParent.substring(commonPrefix.length() + 1)) >> selectedChild
        1 * snapshot.type >> fileType
        interaction { noMoreInteractions() }

        where:
        vfsSpecWithFileType << [COMMON_PREFIX, [FileType.RegularFile, FileType.Directory]].combinations()
        vfsSpec = vfsSpecWithFileType[0] as VirtualFileSystemTestSpec
        fileType = vfsSpecWithFileType[1] as FileType
    }

    def "store Missing child with common prefix adds a new child with the shared prefix with unknown metadata (#vfsSpec)"() {
        setupTest(vfsSpec)
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        AbstractIncompleteSnapshotWithChildren newChild = getNodeWithIndexOfSelectedChild(resultRoot.children)
        then:
        resultRoot.children == childrenWithSelectedChildReplacedBy(newChild)
        newChild.pathToParent == commonPrefix
        newChild.children == sortedChildren(snapshot, selectedChild)
        !(newChild instanceof MetadataSnapshot)
        1 * snapshot.withPathToParent(snapshot.pathToParent.substring(commonPrefix.length() + 1)) >> snapshot
        1 * selectedChild.withPathToParent(selectedChild.pathToParent.substring(commonPrefix.length() + 1)) >> selectedChild
        1 * snapshot.type >> FileType.Missing
        interaction { noMoreInteractions() }

        where:
        vfsSpec << COMMON_PREFIX
    }

    def "store child with no common prefix adds it (#vfsSpec)"() {
        setupTest(vfsSpec)
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(newPathToParent)
        def newChild = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        then:
        resultRoot.children == childrenWithAdditionalChild(newChild)
        isSameNodeType(resultRoot)
        1 * snapshot.withPathToParent(newPathToParent) >> newChild
        interaction { noMoreInteractions() }

        where:
        vfsSpec << NO_COMMON_PREFIX
    }

    def "store parent #vfsSpec.absolutePath replaces child #vfsSpec.selectedChildPath (#vfsSpec)"() {
        setupTest(vfsSpec)
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(newPathToParent)
        def parent = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        then:
        resultRoot.children == childrenWithSelectedChildReplacedBy(parent)
        isSameNodeType(resultRoot)
        1 * snapshot.withPathToParent(newPathToParent) >> parent
        interaction { noMoreInteractions() }

        where:
        vfsSpec << PARENT_PATH
    }

    def "storing a complete snapshot with same path #vfsSpec.absolutePath does replace child (#vfsSpec)"() {
        setupTest(vfsSpec)
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(CompleteFileSystemLocationSnapshot, newPathToParent)
        def snapshotWithParent = mockSnapshot(CompleteFileSystemLocationSnapshot, newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        then:
        resultRoot.children == childrenWithSelectedChildReplacedBy(snapshotWithParent)
        isSameNodeType(resultRoot)
        1 * snapshot.withPathToParent(newPathToParent) >> snapshotWithParent
        interaction { noMoreInteractions() }

        where:
        vfsSpec << SAME_PATH
    }

    def "storing a metadata snapshot with same path #vfsSpec.absolutePath does not replace a complete snapshot (#vfsSpec)"() {
        setupTest(vfsSpec, { mockSnapshot(CompleteFileSystemLocationSnapshot, it) })
        def newPathToParent = absolutePath.substring(offset)
        def snapshot = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshot)
        then:
        resultRoot.children == children
        isSameNodeType(resultRoot)
        interaction {
            getSelectedChildSnapshot()
            noMoreInteractions()
        }

        where:
        vfsSpec << SAME_PATH
    }

    def "storing a metadata snapshot with same path #vfsSpec.absolutePath does replace a metadata snapshot (#vfsSpec)"() {
        setupTest(vfsSpec, { mockSnapshot(MetadataSnapshot, it) })
        def newPathToParent = absolutePath.substring(offset)
        def newSnapshot = mockSnapshot(newPathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, newSnapshot)
        then:
        isSameNodeType(resultRoot)
        resultRoot.children == childrenWithSelectedChildReplacedBy(newSnapshot)
        interaction {
            getSelectedChildSnapshot()
            1 * newSnapshot.withPathToParent(newPathToParent) >> newSnapshot
            noMoreInteractions()
        }

        where:
        vfsSpec << SAME_PATH
    }

    def "storing the child #vfsSpec.absolutePath of #vfsSpec.selectedChildPath updates the child (#vfsSpec)"() {
        setupTest(vfsSpec)
        def snapshotToStore = mockSnapshot(selectedChild.pathToParent)
        def updatedChildNode = mockNode(selectedChild.pathToParent)

        when:
        def resultRoot = initialRoot.store(absolutePath, offset, snapshotToStore)
        then:
        initialRoot.class == resultRoot.class
        resultRoot.children == childrenWithSelectedChildReplacedBy(updatedChildNode)
        interaction {
            storeDescendantOfSelectedChild(snapshotToStore, updatedChildNode)
            noMoreInteractions()
        }

        where:
        vfsSpec << DESCENDANT_PATH
    }

    def storeDescendantOfSelectedChild(MetadataSnapshot snapshot, FileSystemNode updatedChild) {
        def descendantOffset = offset + selectedChild.pathToParent.length() + 1
        1 * selectedChild.store(absolutePath, descendantOffset, snapshot) >> updatedChild
    }

    def "querying the snapshot for non-existing child #vfsSpec.absolutePath finds nothings (#vfsSpec)"() {
        setupTest(vfsSpec)

        when:
        def resultRoot = initialRoot.getSnapshot(absolutePath, offset)
        then:
        !resultRoot.present
        interaction { noMoreInteractions() }

        where:
        vfsSpec << NO_COMMON_PREFIX + COMMON_PREFIX + PARENT_PATH
    }

    def "querying the snapshot for existing child #vfsSpec.absolutePath returns the snapshot for the child (#vfsSpec)"() {
        setupTest(vfsSpec, { mockSnapshot(MetadataSnapshot, it) })

        when:
        def resultRoot = initialRoot.getSnapshot(absolutePath, offset)
        then:
        resultRoot.get() == selectedChild
        interaction {
            getSelectedChildSnapshot()
            noMoreInteractions()
        }

        where:
        vfsSpec << SAME_PATH
    }

    def "querying the snapshot for existing child #vfsSpec.absolutePath without snapshot returns empty (#vfsSpec)"() {
        setupTest(vfsSpec, { mockSnapshot(MetadataSnapshot, it) })

        when:
        def resultRoot = initialRoot.getSnapshot(absolutePath, offset)
        then:
        !resultRoot.present
        interaction {
            getSelectedChildSnapshot(null)
            noMoreInteractions()
        }

        where:
        vfsSpec << SAME_PATH
    }

    def "querying the snapshot for descendant of child #vfsSpec.selectedChildPath queries the child (#vfsSpec)"() {
        setupTest(vfsSpec)
        def descendantSnapshot = mockSnapshot(absolutePath.substring(selectedChild.pathToParent.length() + 1))

        when:
        def resultRoot = initialRoot.getSnapshot(absolutePath, offset)
        then:
        resultRoot.get() == descendantSnapshot
        interaction {
            getDescendantSnapshotOfSelectedChild(descendantSnapshot)
            noMoreInteractions()
        }

        where:
        vfsSpec << DESCENDANT_PATH
    }

    def "querying the snapshot for non-existing descendant of child #vfsSpec.selectedChildPath returns empty (#vfsSpec)"() {
        setupTest(vfsSpec)

        when:
        def resultRoot = initialRoot.getSnapshot(absolutePath, offset)
        then:
        !resultRoot.present
        interaction {
            getDescendantSnapshotOfSelectedChild(null)
            noMoreInteractions()
        }

        where:
        vfsSpec << DESCENDANT_PATH
    }

    static final CHILD_CONSTELLATIONS = [
        ['name'],
        ['name', 'name1'],
        ['name', 'name1', 'name12'],
        ['name', 'name1', 'name12', 'name2', 'name21'],
        ['name/some'],
        ['name/some/other'],
        ['name/some', 'name2'],
        ['name/some', 'name2/other'],
        ['name/some', 'name2/other'],
        ['name', 'name1/some', 'name2/other/third'],
        ['aa/b1', 'ab/a1', 'name', 'name1/some', 'name2/other/third']
    ]
    static final List<VirtualFileSystemTestSpec> NO_COMMON_PREFIX = CHILD_CONSTELLATIONS.collectMany { childPaths ->
        childPaths.collect { childPath ->
            def firstSlash = childPath.indexOf('/')
            String newChildPath = firstSlash > -1
                ? "${childPath.substring(0, firstSlash)}0${childPath.substring(firstSlash)}"
                : "${childPath}0"
            new VirtualFileSystemTestSpec(childPaths, newChildPath, 0, null)
        } + new VirtualFileSystemTestSpec(childPaths, 'completelyDifferent', 0, null)
    }
    static final List<VirtualFileSystemTestSpec> COMMON_PREFIX = CHILD_CONSTELLATIONS.collectMany { childPaths ->
        childPaths.findAll { it.contains('/') } collectMany { childPath ->
            parentPaths(childPath).collect {
                new VirtualFileSystemTestSpec(childPaths, "${it}/different", 0, childPath)
            }
        }
    }
    static final List<VirtualFileSystemTestSpec> PARENT_PATH = CHILD_CONSTELLATIONS.collectMany { childPaths ->
        childPaths.findAll { it.contains('/') } collectMany { childPath ->
            parentPaths(childPath).collect { parentPath ->
                new VirtualFileSystemTestSpec(childPaths, parentPath, 0, findPathWithParent(childPaths, parentPath))
            }
        }
    }
    static final List<VirtualFileSystemTestSpec> SAME_PATH = CHILD_CONSTELLATIONS.collectMany { childPaths ->
        childPaths.collect {
            new VirtualFileSystemTestSpec(childPaths, it, 0, it)
        }
    }
    static final List<VirtualFileSystemTestSpec> DESCENDANT_PATH = CHILD_CONSTELLATIONS.collectMany { childPaths ->
        childPaths.collect {
            new VirtualFileSystemTestSpec(childPaths, "${it}/descendant", 0, it)
        }
    }

    def getSelectedChildSnapshot(@Nullable FileSystemNode foundSnapshot = selectedChild) {
        1 * selectedChild.getSnapshot(absolutePath, absolutePath.length() + 1) >> Optional.ofNullable(foundSnapshot)
    }

    def getDescendantSnapshotOfSelectedChild(@Nullable MetadataSnapshot foundSnapshot) {
        def descendantOffset = offset + selectedChild.pathToParent.length() + 1
        1 * selectedChild.getSnapshot(absolutePath, descendantOffset) >> Optional.ofNullable(foundSnapshot)
    }

    def invalidateDescendantOfSelectedChild(@Nullable FileSystemNode invalidatedChild) {
        def descendantOffset = offset + selectedChild.pathToParent.length() + 1
        1 * selectedChild.invalidate(absolutePath, descendantOffset) >> Optional.ofNullable(invalidatedChild)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    void noMoreInteractions() {
        _ * _.pathToParent
        0 * _
    }

    private static String findPathWithParent(List<String> childPaths, String parentPath) {
        childPaths.find { PathUtil.isChildOfOrThis(it, 0, parentPath) }
    }

    private static List<String> parentPaths(String childPath) {
        (childPath.split('/') as List).inits().tail().init().collect { it.join('/') }
    }


    static List<FileSystemNode> sortedChildren(FileSystemNode... children) {
        def childrenOfIntermediate = children as List
        childrenOfIntermediate.sort(Comparator.comparing({ FileSystemNode node -> node.pathToParent }, PathUtil.pathComparator()))
        childrenOfIntermediate
    }

    FileSystemNode mockNode(String pathToParent) {
        Mock(FileSystemNode, defaultResponse: new RespondWithPathToParent(pathToParent), name: pathToParent)
    }

    def <T extends FileSystemNode> T mockSnapshot(
        Class<T> type,
        String pathToParent
    ) {
        Mock(type, defaultResponse: new RespondWithPathToParent(pathToParent), name: pathToParent)
    }

    MetadataSnapshot mockSnapshot(String pathToParent) {
        mockSnapshot(MetadataSnapshot, pathToParent)
    }

    void setupTest(VirtualFileSystemTestSpec spec, SelectedChildCreator selectedChildCreator = null) {
        def children = createChildren(spec.childPaths, spec.selectedChildPath, selectedChildCreator)
        def initialRoot = createInitialRootNode("path/to/parent", children)
        this.initialRoot = initialRoot
        this.children = children
        this.absolutePath = spec.absolutePath
        this.offset = spec.offset
        this.selectedChild = children.find { it.pathToParent == spec.selectedChildPath }
    }

    List<FileSystemNode> createChildren(List<String> pathsToParent, @Nullable String selectedChildPath = null, @Nullable SelectedChildCreator selectedChildCreator = null) {
        return pathsToParent.stream()
            .sorted(PathUtil.pathComparator())
            .map{childPath -> selectedChildCreator != null && selectedChildPath == childPath
                    ? selectedChildCreator.createSelectedChild(childPath)
                    : mockNode(childPath)
            }
            .collect(Collectors.toList())
    }

    interface SelectedChildCreator {
        FileSystemNode createSelectedChild(String pathToParent)
    }

    List<FileSystemNode> childrenWithSelectedChildReplacedBy(FileSystemNode replacement) {
        children.collect { it == selectedChild ? replacement : it }
    }

    List<FileSystemNode> childrenWithAdditionalChild(FileSystemNode newChild) {
        List<FileSystemNode> newChildren = new ArrayList<>(children)
        newChildren.add(insertionPoint, newChild)
        return newChildren
    }

    List<FileSystemNode> childrenWithSelectedChildRemoved() {
        children.findAll { it != selectedChild }
    }

    FileSystemNode getNodeWithIndexOfSelectedChild(List<FileSystemNode> newChildren) {
        int index = children.indexOf(selectedChild)
        return newChildren[index]
    }

    private int getInsertionPoint() {
        int childIndex = SearchUtil.binarySearch(children) {
            candidate -> PathUtil.compareWithCommonPrefix(candidate.pathToParent, absolutePath, offset)
        }
        assert childIndex < 0
        return -childIndex - 1
    }

    String getCommonPrefix() {
        return selectedChild.pathToParent.substring(0, PathUtil.sizeOfCommonPrefix(selectedChild.pathToParent, absolutePath, offset))
    }
}
