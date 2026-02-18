"""Tests for A2UI model classes and parsing."""

import pytest

from ..models import (
    A2UISurface,
    BeginRendering,
    ComponentInstance,
    DataModelEntry,
    DataModelUpdate,
    ExplicitList,
    SurfaceUpdate,
    build_surface_from_messages,
    parse_bound_value,
    parse_component_instance,
    parse_container_children,
)


class TestParseBoundValue:
    def test_literal_string(self):
        bv = parse_bound_value({"literalString": "Hello"})
        assert bv.literal_string == "Hello"
        assert bv.path is None
        assert bv.is_literal_only()

    def test_path_only(self):
        bv = parse_bound_value({"path": "/user/name"})
        assert bv.path == "/user/name"
        assert bv.is_path_only()

    def test_init_shorthand(self):
        bv = parse_bound_value({"path": "/user/name", "literalString": "Guest"})
        assert bv.is_init_shorthand()


class TestParseContainerChildren:
    def test_explicit_list(self):
        ch = parse_container_children({"explicitList": ["a", "b", "c"]})
        assert ch.explicit_list is not None
        assert ch.explicit_list.ids == ["a", "b", "c"]
        assert ch.template is None

    def test_template(self):
        ch = parse_container_children({
            "template": {"dataBinding": "/items", "componentId": "item_tpl"}
        })
        assert ch.template is not None
        assert ch.template.data_binding == "/items"
        assert ch.template.component_id == "item_tpl"

    def test_missing_both_raises(self):
        with pytest.raises(ValueError, match="explicitList or template"):
            parse_container_children({})


class TestParseComponentInstance:
    def test_text_component(self):
        wire = {
            "id": "txt1",
            "component": {"Text": {"text": {"literalString": "Hi"}}},
        }
        c = parse_component_instance(wire)
        assert c.id == "txt1"
        assert c.component_type == "Text"
        assert "text" in c.component_props


class TestBuildSurfaceFromMessages:
    def test_surface_update_and_begin_rendering(self):
        messages = [
            SurfaceUpdate(
                components=[
                    ComponentInstance("root", "Column", {"children": ExplicitList(ids=["txt"])}),
                    ComponentInstance("txt", "Text", {"text": {"literalString": "Hello"}}),
                ]
            ),
            BeginRendering(root="root"),
        ]
        surf = build_surface_from_messages(messages)
        assert surf.root_id == "root"
        assert "root" in surf.components
        assert "txt" in surf.components

    def test_data_model_update_merge(self):
        messages = [
            DataModelUpdate(path="user", contents=[
                DataModelEntry(key="name", value_string="Bob"),
                DataModelEntry(key="age", value_number=30),
            ]),
        ]
        surf = build_surface_from_messages(messages)
        assert surf.data_model.get("user", {}).get("name") == "Bob"
        assert surf.data_model.get("user", {}).get("age") == 30
