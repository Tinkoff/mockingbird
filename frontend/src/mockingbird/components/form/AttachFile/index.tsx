import React, { useCallback } from 'react';
import type { Control } from 'react-hook-form';
import { useController } from 'react-hook-form';
import PlatformAttachFile from '@platform-ui/attachFile';

interface Props {
  name: string;
  control: Control;
  required?: boolean;
  single?: boolean;
}

export default function AttachFile(props: Props) {
  const { name, control, required = false, single = false } = props;
  const { field } = useController({
    name,
    control,
    rules: {
      required,
    },
  });
  const { value, onChange } = field;
  const onAdd = useCallback(
    (_, { files }) => {
      onChange(files.map((f) => ({ ...f, status: 'success' })));
    },
    [onChange]
  );
  const handleRemove = useCallback(
    (_, { file }) => {
      onChange(value.filter((f) => f !== file));
    },
    [onChange, value]
  );
  return (
    <PlatformAttachFile
      {...field}
      files={value}
      single={single}
      onAdd={onAdd}
      onRemove={handleRemove}
    />
  );
}
